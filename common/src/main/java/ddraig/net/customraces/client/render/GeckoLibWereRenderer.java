package ddraig.net.customraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import ddraig.net.customraces.data.RaceData;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Robust, reflection-backed GeckoLib model loader and bone renderer for Custom Races.
 * Dynamically loads and renders ANY GeckoLib .geo.json model (bears, cats, dragons, beasts, etc.)
 * safely across Fabric and Forge environments without hardcoded mesh assumptions.
 */
public class GeckoLibWereRenderer {

    public static boolean isModelPresent(ResourceLocation modelLoc, String rawPath) {
        if (modelLoc == null) return false;
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            Method getModelsMethod = cacheClass.getMethod("getBakedModels");
            Map<?, ?> bakedModels = (Map<?, ?>) getModelsMethod.invoke(null);
            Object bakedModel = bakedModels != null ? bakedModels.get(modelLoc) : null;
            if (bakedModel == null) {
                bakedModel = bakeModelFromFile(modelLoc, rawPath);
            }
            if (bakedModel == null) return false;

            Method topLevelBonesMethod = bakedModel.getClass().getMethod("topLevelBones");
            List<?> topBones = (List<?>) topLevelBonesMethod.invoke(bakedModel);
            return topBones != null && !topBones.isEmpty();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isModelPresent(ResourceLocation modelLoc) {
        return isModelPresent(modelLoc, null);
    }

    public static boolean renderGeckoModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, RaceData race, ResourceLocation modelLoc, ResourceLocation textureLoc, ResourceLocation animLoc, float netHeadYaw, float headPitch) {
        if (modelLoc == null) return false;
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            Method getModelsMethod = cacheClass.getMethod("getBakedModels");
            Map<?, ?> bakedModels = (Map<?, ?>) getModelsMethod.invoke(null);
            
            Object bakedModel = bakedModels != null ? bakedModels.get(modelLoc) : null;
            if (bakedModel == null) {
                bakedModel = bakeModelFromFile(modelLoc, race != null ? race.wereModelPath : null);
            }
            if (bakedModel == null) return false;

            Method topLevelBonesMethod = bakedModel.getClass().getMethod("topLevelBones");
            List<?> topBones = (List<?>) topLevelBonesMethod.invoke(bakedModel);
            if (topBones == null || topBones.isEmpty()) return false;

            // Evaluate active keyframe animation state from player state variables
            String activeAnim = resolveActiveAnimation(player, race);
            if (animLoc != null) {
                bakeAnimationsFromFile(animLoc, race != null ? race.wereAnimationPath : null);
            }

            // Invisibility Effect & Spectator Mode Handling
            boolean isInvisible = player != null && (player.isInvisible() || player.isSpectator());
            if (isInvisible) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                net.minecraft.client.player.LocalPlayer clientPlayer = mc != null ? mc.player : null;
                if (clientPlayer != null && player.isInvisibleTo(clientPlayer)) {
                    // Player is completely invisible to viewing player: return true (handled) drawing nothing
                    return true;
                }
            }

            RenderType renderType = isInvisible ? RenderType.entityTranslucent(textureLoc) : RenderType.entityCutoutNoCull(textureLoc);
            VertexConsumer vc = buffer.getBuffer(renderType);
            float alpha = isInvisible ? 0.15f : 1.0f;

            poseStack.pushPose();
            int[] verticesDrawn = new int[1];
            try {
                // Align GeckoLib model origin to entity feet (0.0, 0.0, 0.0)
                for (Object bone : topBones) {
                    renderBoneReflect(poseStack, vc, bone, packedLight, player, netHeadYaw, headPitch, alpha, verticesDrawn);
                }
            } finally {
                poseStack.popPose();
            }

            if (verticesDrawn[0] == 0) {
                System.err.println("[CustomRaces] GeckoLib model " + modelLoc + " rendered 0 quads/vertices. Falling back to base character model.");
                return false;
            }
            return true;
        } catch (Throwable t) {
            System.err.println("[CustomRaces] GeckoLib model rendering exception for " + modelLoc + ": " + t.getMessage());
            return false;
        }
    }

    /**
     * Maps player state variables to configured GeckoLib keyframe animation trigger keys.
     * Priority Hierarchy: Hurt > Attack > Swimming > Flying > Walk > Idle
     */
    private static String sanitizeAnimKey(String rawKey, String fallbackKey) {
        if (rawKey == null || rawKey.trim().isEmpty()) {
            return fallbackKey;
        }
        String key = rawKey.trim();
        if (key.endsWith(".animation.json") || key.contains(":") || key.contains("/")) {
            String filename = key.replaceAll(".*/", "").replaceAll(".*:", "");
            if (filename.endsWith(".animation.json")) {
                filename = filename.substring(0, filename.length() - ".animation.json".length());
            }
            if (filename.endsWith(".json")) {
                filename = filename.substring(0, filename.length() - 5);
            }
            if (!filename.trim().isEmpty()) {
                return "animation." + filename.trim() + "." + fallbackKey.replaceAll(".*\\.", "");
            }
            return fallbackKey;
        }
        return key;
    }

    /**
     * Maps player state variables to configured GeckoLib keyframe animation trigger keys.
     * Priority Hierarchy: Hurt > Attack > Swimming > Flying > Walk > Idle
     */
    public static String resolveActiveAnimation(AbstractClientPlayer player, RaceData race) {
        if (player == null) {
            String raw = race != null ? race.getSafeWereIdleAnim() : "animation.were.idle";
            return sanitizeAnimKey(raw, "animation.were.idle");
        }

        // 1. Hurt Animation (taking damage)
        if (player.hurtTime > 0) {
            String raw = race != null ? race.getSafeWereHurtAnim() : "animation.were.hurt";
            return sanitizeAnimKey(raw, "animation.were.hurt");
        }

        // 2. Attack Animation (swinging attack)
        if (player.swingTime > 0 || player.swinging) {
            String raw = race != null ? race.getSafeWereAttackAnim() : "animation.were.attack";
            return sanitizeAnimKey(raw, "animation.were.attack");
        }

        // 3. Swim Animation (swimming)
        if (player.isVisuallySwimming()) {
            String raw = race != null ? race.getSafeWereSwimAnim() : "animation.were.swim";
            return sanitizeAnimKey(raw, "animation.were.swim");
        }

        // 4. Fly Animation (flying)
        if (player.getAbilities() != null && player.getAbilities().flying) {
            String raw = race != null ? race.getSafeWereFlyAnim() : "animation.were.fly";
            return sanitizeAnimKey(raw, "animation.were.fly");
        }

        // 5. Walk vs Idle Animation based on movement speed threshold (0.01f)
        float speed = 0.0f;
        if (player.walkAnimation != null) {
            speed = player.walkAnimation.speed();
        } else if (player.getDeltaMovement() != null) {
            speed = (float) Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x + player.getDeltaMovement().z * player.getDeltaMovement().z);
        }

        if (speed >= 0.01f) {
            String raw = race != null ? race.getSafeWereWalkAnim() : "animation.were.walk";
            return sanitizeAnimKey(raw, "animation.were.walk");
        } else {
            String raw = race != null ? race.getSafeWereIdleAnim() : "animation.were.idle";
            return sanitizeAnimKey(raw, "animation.were.idle");
        }
    }

    public static boolean renderGeckoModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, RaceData race, ResourceLocation modelLoc, ResourceLocation textureLoc, ResourceLocation animLoc) {
        return renderGeckoModel(poseStack, buffer, packedLight, player, race, modelLoc, textureLoc, animLoc, 0.0f, 0.0f);
    }

    private static boolean isHeadBone(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return lower.equals("head") || lower.equals("bipedhead") || lower.equals("head_bone") || lower.equals("headbone");
    }

    private static Iterable<?> toIterable(Object obj) {
        if (obj == null) return java.util.Collections.emptyList();
        if (obj instanceof Iterable<?> iter) {
            return iter;
        }
        if (obj.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(obj);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(java.lang.reflect.Array.get(obj, i));
            }
            return list;
        }
        return java.util.Collections.emptyList();
    }

    private static float[] extractPos(Object posObj) {
        if (posObj == null) return null;
        if (posObj instanceof float[] fArr && fArr.length >= 3) {
            return fArr;
        }
        if (posObj instanceof double[] dArr && dArr.length >= 3) {
            return new float[]{(float) dArr[0], (float) dArr[1], (float) dArr[2]};
        }
        try {
            Method xM = posObj.getClass().getMethod("x");
            Method yM = posObj.getClass().getMethod("y");
            Method zM = posObj.getClass().getMethod("z");
            float x = ((Number) xM.invoke(posObj)).floatValue();
            float y = ((Number) yM.invoke(posObj)).floatValue();
            float z = ((Number) zM.invoke(posObj)).floatValue();
            return new float[]{x, y, z};
        } catch (Throwable t1) {
            try {
                Field xF = posObj.getClass().getField("x");
                Field yF = posObj.getClass().getField("y");
                Field zF = posObj.getClass().getField("z");
                float x = xF.getFloat(posObj);
                float y = yF.getFloat(posObj);
                float z = zF.getFloat(posObj);
                return new float[]{x, y, z};
            } catch (Throwable t2) {
                try {
                    Method getXM = posObj.getClass().getMethod("getX");
                    Method getYM = posObj.getClass().getMethod("getY");
                    Method getZM = posObj.getClass().getMethod("getZ");
                    float x = ((Number) getXM.invoke(posObj)).floatValue();
                    float y = ((Number) getYM.invoke(posObj)).floatValue();
                    float z = ((Number) getZM.invoke(posObj)).floatValue();
                    return new float[]{x, y, z};
                } catch (Throwable t3) {
                    return null;
                }
            }
        }
    }

    private static float getFloatReflect(Object obj, String method1, String method2, String fieldName) {
        if (obj == null) return 0.0f;
        try {
            Method m = obj.getClass().getMethod(method1);
            return ((Number) m.invoke(obj)).floatValue();
        } catch (Throwable t1) {
            try {
                Method m = obj.getClass().getMethod(method2);
                return ((Number) m.invoke(obj)).floatValue();
            } catch (Throwable t2) {
                try {
                    Field f = obj.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.getFloat(obj);
                } catch (Throwable t3) {
                    return 0.0f;
                }
            }
        }
    }

    private static void renderBoneReflect(PoseStack poseStack, VertexConsumer vc, Object bone, int packedLight, AbstractClientPlayer player, float netHeadYaw, float headPitch, float alpha, int[] verticesDrawn) {
        if (bone == null) return;
        try {
            Method isHiddenMethod = bone.getClass().getMethod("isHidden");
            if ((Boolean) isHiddenMethod.invoke(bone)) return;

            String boneName = null;
            try {
                Method getNameMethod = bone.getClass().getMethod("getName");
                boneName = (String) getNameMethod.invoke(bone);
            } catch (Throwable ignored) {}

            float pivX = getFloatReflect(bone, "getPivotX", "pivotX", "pivotX");
            float pivY = getFloatReflect(bone, "getPivotY", "pivotY", "pivotY");
            float pivZ = getFloatReflect(bone, "getPivotZ", "pivotZ", "pivotZ");

            float px = getFloatReflect(bone, "getPosX", "posX", "posX");
            float py = getFloatReflect(bone, "getPosY", "posY", "posY");
            float pz = getFloatReflect(bone, "getPosZ", "posZ", "posZ");

            float rx = getFloatReflect(bone, "getRotX", "rotX", "rotX");
            float ry = getFloatReflect(bone, "getRotY", "rotY", "rotY");
            float rz = getFloatReflect(bone, "getRotZ", "rotZ", "rotZ");

            float sx = getFloatReflect(bone, "getScaleX", "scaleX", "scaleX");
            float sy = getFloatReflect(bone, "getScaleY", "scaleY", "scaleY");
            float sz = getFloatReflect(bone, "getScaleZ", "scaleZ", "scaleZ");

            if (sx == 0.0f) sx = 1.0f;
            if (sy == 0.0f) sy = 1.0f;
            if (sz == 0.0f) sz = 1.0f;

            poseStack.pushPose();
            try {
                // 1. Move to bone position + pivot origin
                poseStack.translate((px + pivX) / 16.0f, (py + pivY) / 16.0f, (pz + pivZ) / 16.0f);

                // 2. Apply Euler rotations around joint pivot
                if (rz != 0.0f) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-rz));
                if (ry != 0.0f) poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-ry));
                if (rx != 0.0f) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rx));

                // Apply rotational matrix transforms when traversing head bones
                if (isHeadBone(boneName)) {
                    if (netHeadYaw != 0.0f) {
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(netHeadYaw));
                    }
                    if (headPitch != 0.0f) {
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(headPitch));
                    }
                }

                // 3. Apply bone scaling
                if (sx != 1.0f || sy != 1.0f || sz != 1.0f) {
                    poseStack.scale(sx, sy, sz);
                }

                // 4. Translate back by pivot offset
                poseStack.translate(-pivX / 16.0f, -pivY / 16.0f, -pivZ / 16.0f);

                Object cubesObj = null;
                try {
                    Method getCubes = bone.getClass().getMethod("getCubes");
                    cubesObj = getCubes.invoke(bone);
                } catch (Throwable t) {
                    try {
                        Field f = bone.getClass().getDeclaredField("cubes");
                        f.setAccessible(true);
                        cubesObj = f.get(bone);
                    } catch (Throwable ignored) {}
                }

                for (Object cube : toIterable(cubesObj)) {
                    renderCubeReflect(poseStack, vc, cube, packedLight, player, alpha, verticesDrawn);
                }

                Object childBonesObj = null;
                try {
                    Method getChildBones = bone.getClass().getMethod("getChildBones");
                    childBonesObj = getChildBones.invoke(bone);
                } catch (Throwable t) {
                    try {
                        Field f = bone.getClass().getDeclaredField("childBones");
                        f.setAccessible(true);
                        childBonesObj = f.get(bone);
                    } catch (Throwable ignored) {}
                }

                for (Object child : toIterable(childBonesObj)) {
                    renderBoneReflect(poseStack, vc, child, packedLight, player, netHeadYaw, headPitch, alpha, verticesDrawn);
                }
            } finally {
                poseStack.popPose();
            }
        } catch (Throwable ignored) {}
    }

    private static void renderCubeReflect(PoseStack poseStack, VertexConsumer vc, Object cube, int packedLight, AbstractClientPlayer player, float alpha, int[] verticesDrawn) {
        if (cube == null) return;
        try {
            Object quadsObj = null;
            try {
                Method quadsMethod = cube.getClass().getMethod("quads");
                quadsObj = quadsMethod.invoke(cube);
            } catch (Throwable ignored) {
                try {
                    Field quadsField = cube.getClass().getDeclaredField("quads");
                    quadsField.setAccessible(true);
                    quadsObj = quadsField.get(cube);
                } catch (Throwable ignored2) {}
            }

            if (quadsObj == null) return;
            org.joml.Matrix4f pose = poseStack.last().pose();
            org.joml.Matrix3f normal = poseStack.last().normal();
            boolean isHurt = player != null && player.hurtTime > 0;
            int overlay = isHurt ? OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(true)) : OverlayTexture.NO_OVERLAY;
            float rMult = 1.0f;
            float gMult = isHurt ? 0.35f : 1.0f;
            float bMult = isHurt ? 0.35f : 1.0f;

            for (Object quad : toIterable(quadsObj)) {
                if (quad == null) continue;
                net.minecraft.core.Direction dir = null;
                try {
                    Method dirMethod = quad.getClass().getMethod("direction");
                    dir = (net.minecraft.core.Direction) dirMethod.invoke(quad);
                } catch (Throwable ignored) {}

                float nx = dir != null ? dir.getStepX() : 0.0f;
                float ny = dir != null ? dir.getStepY() : 1.0f;
                float nz = dir != null ? dir.getStepZ() : 0.0f;

                Object verticesObj = null;
                try {
                    Method verticesMethod = quad.getClass().getMethod("vertices");
                    verticesObj = verticesMethod.invoke(quad);
                } catch (Throwable ignored) {
                    try {
                        Field verticesField = quad.getClass().getDeclaredField("vertices");
                        verticesField.setAccessible(true);
                        verticesObj = verticesField.get(quad);
                    } catch (Throwable ignored2) {}
                }

                if (verticesObj == null) continue;
                for (Object vertex : toIterable(verticesObj)) {
                    if (vertex == null) continue;
                    Object posObj = null;
                    try {
                        Method posMethod = vertex.getClass().getMethod("position");
                        posObj = posMethod.invoke(vertex);
                    } catch (Throwable ignored) {
                        try {
                            Field posField = vertex.getClass().getDeclaredField("position");
                            posField.setAccessible(true);
                            posObj = posField.get(vertex);
                        } catch (Throwable ignored2) {}
                    }

                    float[] pos = extractPos(posObj);
                    float u = getFloatReflect(vertex, "u", "getU", "u");
                    float v = getFloatReflect(vertex, "v", "getV", "v");

                    if (pos != null && pos.length >= 3) {
                        vc.vertex(pose, pos[0] / 16.0f, pos[1] / 16.0f, pos[2] / 16.0f)
                                .color(rMult, gMult, bMult, alpha)
                                .uv(u, v)
                                .overlayCoords(overlay)
                                .uv2(packedLight)
                                .normal(normal, nx, ny, nz)
                                .endVertex();
                        verticesDrawn[0]++;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static Object bakeModelFromFile(ResourceLocation modelLoc, String rawPath) {
        if (modelLoc == null) return null;
        try {
            String content = GeckoAssetResolver.getModelContent(modelLoc, rawPath);
            if (content != null && !content.trim().isEmpty()) {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
                Class<?> jsonUtilClass = Class.forName("software.bernie.geckolib.util.JsonUtil");
                Object geoGson = jsonUtilClass.getField("GEO_GSON").get(null);
                Method fromJsonMethod = geoGson.getClass().getMethod("fromJson", com.google.gson.JsonElement.class, Class.class);

                Class<?> rawModelClass = Class.forName("software.bernie.geckolib.loading.json.raw.Model");
                Object rawModel = fromJsonMethod.invoke(geoGson, json, rawModelClass);

                Class<?> geomTreeClass = Class.forName("software.bernie.geckolib.loading.object.GeometryTree");
                Method fromModelMethod = geomTreeClass.getMethod("fromModel", rawModelClass);
                Object geomTree = fromModelMethod.invoke(null, rawModel);

                Class<?> bakedFactoryClass = Class.forName("software.bernie.geckolib.loading.object.BakedModelFactory");
                Method getForNsMethod = bakedFactoryClass.getMethod("getForNamespace", String.class);
                Object factory = getForNsMethod.invoke(null, modelLoc.getNamespace());

                Method constructGeoModelMethod = factory.getClass().getMethod("constructGeoModel", geomTreeClass);
                Object bakedModel = constructGeoModelMethod.invoke(factory, geomTree);

                if (bakedModel != null) {
                    Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
                    Method getModelsMethod = cacheClass.getMethod("getBakedModels");
                    Map<Object, Object> bakedModels = (Map<Object, Object>) getModelsMethod.invoke(null);
                    if (bakedModels != null) {
                        bakedModels.put(modelLoc, bakedModel);
                    }
                    return bakedModel;
                }
            }
        } catch (Throwable t) {
            System.err.println("[CustomRaces] Dynamic GeckoLib model baking failed for " + modelLoc + ": " + t.getMessage());
        }
        return null;
    }

    public static Object bakeModelFromFile(ResourceLocation modelLoc) {
        return bakeModelFromFile(modelLoc, null);
    }

    public static Object bakeAnimationsFromFile(ResourceLocation animLoc, String rawPath) {
        if (animLoc == null) return null;
        try {
            String content = GeckoAssetResolver.getAnimationContent(animLoc, rawPath);
            if (content != null && !content.trim().isEmpty()) {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
                Class<?> jsonUtilClass = Class.forName("software.bernie.geckolib.util.JsonUtil");
                Object geoGson = jsonUtilClass.getField("GEO_GSON").get(null);
                Method fromJsonMethod = geoGson.getClass().getMethod("fromJson", com.google.gson.JsonElement.class, Class.class);

                Class<?> bakedAnimsClass = Class.forName("software.bernie.geckolib.loading.object.BakedAnimations");
                Object bakedAnimations = fromJsonMethod.invoke(geoGson, json.getAsJsonObject("animations"), bakedAnimsClass);

                if (bakedAnimations != null) {
                    Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
                    Method getAnimsMethod = cacheClass.getMethod("getBakedAnimations");
                    Map<Object, Object> animMap = (Map<Object, Object>) getAnimsMethod.invoke(null);
                    if (animMap != null) {
                        animMap.put(animLoc, bakedAnimations);
                    }
                    return bakedAnimations;
                }
            }
        } catch (Throwable t) {
            System.err.println("[CustomRaces] Dynamic GeckoLib animation baking failed for " + animLoc + ": " + t.getMessage());
        }
        return null;
    }

    public static Object bakeAnimationsFromFile(ResourceLocation animLoc) {
        return bakeAnimationsFromFile(animLoc, null);
    }
}
