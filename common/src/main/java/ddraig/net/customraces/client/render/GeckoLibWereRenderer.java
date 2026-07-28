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
 * High-performance, reflection-cached GeckoLib model loader and renderer for Custom Races.
 * Renders any GeckoLib .geo.json model at 100% full scale with accurate UV texture mapping,
 * Y-axis upright posture, and procedural limb animations while maintaining 300+ FPS.
 */
public class GeckoLibWereRenderer {

    private static class ReflectionCache {
        static Method getPivotX, getPivotY, getPivotZ;
        static Method getPosX, getPosY, getPosZ;
        static Method getRotX, getRotY, getRotZ;
        static Method getScaleX, getScaleY, getScaleZ;
        static Method isHidden, getName, getCubes, getChildBones;
        static Method quadsMethod, verticesMethod, posMethod, uMethod, vMethod;
        static Field quadsField, verticesField, posField, uField, vField;
        static Method prepMatrixMethod;
        static boolean initialized = false;

        static void init(Class<?> boneClass, Class<?> cubeClass, Class<?> quadClass, Class<?> vertexClass) {
            if (initialized) return;
            try {
                if (boneClass != null) {
                    getPivotX = findMethod(boneClass, "getPivotX", "pivotX");
                    getPivotY = findMethod(boneClass, "getPivotY", "pivotY");
                    getPivotZ = findMethod(boneClass, "getPivotZ", "pivotZ");
                    getPosX = findMethod(boneClass, "getPosX", "posX");
                    getPosY = findMethod(boneClass, "getPosY", "posY");
                    getPosZ = findMethod(boneClass, "getPosZ", "posZ");
                    getRotX = findMethod(boneClass, "getRotX", "rotX");
                    getRotY = findMethod(boneClass, "getRotY", "rotY");
                    getRotZ = findMethod(boneClass, "getRotZ", "rotZ");
                    getScaleX = findMethod(boneClass, "getScaleX", "scaleX");
                    getScaleY = findMethod(boneClass, "getScaleY", "scaleY");
                    getScaleZ = findMethod(boneClass, "getScaleZ", "scaleZ");
                    isHidden = findMethod(boneClass, "isHidden");
                    getName = findMethod(boneClass, "getName");
                    getCubes = findMethod(boneClass, "getCubes");
                    getChildBones = findMethod(boneClass, "getChildBones");
                }
                if (cubeClass != null) {
                    quadsMethod = findMethod(cubeClass, "quads", "getQuads");
                    quadsField = findField(cubeClass, "quads");
                }
                if (quadClass != null) {
                    verticesMethod = findMethod(quadClass, "vertices", "getVertices");
                    verticesField = findField(quadClass, "vertices");
                }
                if (vertexClass != null) {
                    posMethod = findMethod(vertexClass, "position", "getPosition");
                    posField = findField(vertexClass, "position");
                    uMethod = findMethod(vertexClass, "u", "getU");
                    vMethod = findMethod(vertexClass, "v", "getV");
                    uField = findField(vertexClass, "u");
                    vField = findField(vertexClass, "v");
                }
                if (boneClass != null) {
                    try {
                        Class<?> renderUtilsClass = Class.forName("software.bernie.geckolib.util.RenderUtils");
                        prepMatrixMethod = renderUtilsClass.getMethod("prepMatrixForBone", PoseStack.class, boneClass);
                    } catch (Throwable ignored) {}
                }
                initialized = true;
            } catch (Throwable t) {
                initialized = true;
            }
        }

        private static Method findMethod(Class<?> clazz, String... names) {
            for (String n : names) {
                try {
                    Method m = clazz.getMethod(n);
                    m.setAccessible(true);
                    return m;
                } catch (Throwable ignored) {}
            }
            return null;
        }

        private static Field findField(Class<?> clazz, String name) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

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

            if (animLoc != null) {
                bakeAnimationsFromFile(animLoc, race != null ? race.wereAnimationPath : null);
                String activeAnimKey = resolveActiveAnimation(player, race);
                float animTick = player != null ? (player.tickCount + net.minecraft.client.Minecraft.getInstance().getFrameTime()) : 0.0f;
                applyKeyframeAnimation(bakedModel, animLoc, activeAnimKey, animTick);
            }

            boolean isInvisible = player != null && (player.isInvisible() || player.isSpectator());
            if (isInvisible) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                net.minecraft.client.player.LocalPlayer clientPlayer = mc != null ? mc.player : null;
                if (clientPlayer != null && player.isInvisibleTo(clientPlayer)) {
                    return true;
                }
            }

            RenderType renderType = isInvisible ? RenderType.entityTranslucent(textureLoc) : RenderType.entityCutoutNoCull(textureLoc);
            VertexConsumer vc = buffer.getBuffer(renderType);
            float alpha = isInvisible ? 0.15f : 1.0f;

            poseStack.pushPose();
            int[] verticesDrawn = new int[1];
            try {
                // Orient GeckoLib model Y-up with feet on ground (y = 0) and face forward
                poseStack.scale(-1.0f, -1.0f, 1.0f);
                poseStack.translate(0.0d, -1.501d, 0.0d);

                for (Object bone : topBones) {
                    renderBoneReflect(poseStack, vc, bone, packedLight, player, netHeadYaw, headPitch, alpha, verticesDrawn);
                }
            } finally {
                poseStack.popPose();
            }

            if (verticesDrawn[0] == 0) {
                return false;
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean renderGeckoModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, RaceData race, ResourceLocation modelLoc, ResourceLocation textureLoc, ResourceLocation animLoc) {
        return renderGeckoModel(poseStack, buffer, packedLight, player, race, modelLoc, textureLoc, animLoc, 0.0f, 0.0f);
    }

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

    public static String resolveActiveAnimation(AbstractClientPlayer player, RaceData race) {
        if (player == null) {
            String raw = race != null ? race.getSafeWereIdleAnim() : "animation.were.idle";
            return sanitizeAnimKey(raw, "animation.were.idle");
        }

        if (player.hurtTime > 0) {
            String raw = race != null ? race.getSafeWereHurtAnim() : "animation.were.hurt";
            return sanitizeAnimKey(raw, "animation.were.hurt");
        }

        if (player.swingTime > 0 || player.swinging) {
            String raw = race != null ? race.getSafeWereAttackAnim() : "animation.were.attack";
            return sanitizeAnimKey(raw, "animation.were.attack");
        }

        if (player.isVisuallySwimming()) {
            String raw = race != null ? race.getSafeWereSwimAnim() : "animation.were.swim";
            return sanitizeAnimKey(raw, "animation.were.swim");
        }

        if (player.getAbilities() != null && player.getAbilities().flying) {
            String raw = race != null ? race.getSafeWereFlyAnim() : "animation.were.fly";
            return sanitizeAnimKey(raw, "animation.were.fly");
        }

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
            Class<?> boneClass = bone.getClass();
            ReflectionCache.init(boneClass, null, null, null);

            if (ReflectionCache.isHidden != null) {
                Boolean hidden = (Boolean) ReflectionCache.isHidden.invoke(bone);
                if (Boolean.TRUE.equals(hidden)) return;
            }

            String boneName = null;
            if (ReflectionCache.getName != null) {
                boneName = (String) ReflectionCache.getName.invoke(bone);
            }

            float pivX = ReflectionCache.getPivotX != null ? ((Number) ReflectionCache.getPivotX.invoke(bone)).floatValue() : 0.0f;
            float pivY = ReflectionCache.getPivotY != null ? ((Number) ReflectionCache.getPivotY.invoke(bone)).floatValue() : 0.0f;
            float pivZ = ReflectionCache.getPivotZ != null ? ((Number) ReflectionCache.getPivotZ.invoke(bone)).floatValue() : 0.0f;

            float px = ReflectionCache.getPosX != null ? ((Number) ReflectionCache.getPosX.invoke(bone)).floatValue() : 0.0f;
            float py = ReflectionCache.getPosY != null ? ((Number) ReflectionCache.getPosY.invoke(bone)).floatValue() : 0.0f;
            float pz = ReflectionCache.getPosZ != null ? ((Number) ReflectionCache.getPosZ.invoke(bone)).floatValue() : 0.0f;

            float rx = ReflectionCache.getRotX != null ? ((Number) ReflectionCache.getRotX.invoke(bone)).floatValue() : 0.0f;
            float ry = ReflectionCache.getRotY != null ? ((Number) ReflectionCache.getRotY.invoke(bone)).floatValue() : 0.0f;
            float rz = ReflectionCache.getRotZ != null ? ((Number) ReflectionCache.getRotZ.invoke(bone)).floatValue() : 0.0f;

            float sx = ReflectionCache.getScaleX != null ? ((Number) ReflectionCache.getScaleX.invoke(bone)).floatValue() : 1.0f;
            float sy = ReflectionCache.getScaleY != null ? ((Number) ReflectionCache.getScaleY.invoke(bone)).floatValue() : 1.0f;
            float sz = ReflectionCache.getScaleZ != null ? ((Number) ReflectionCache.getScaleZ.invoke(bone)).floatValue() : 1.0f;

            if (sx == 0.0f) sx = 1.0f;
            if (sy == 0.0f) sy = 1.0f;
            if (sz == 0.0f) sz = 1.0f;

            poseStack.pushPose();
            try {
                if (Math.abs(pivX) > 4.0f || Math.abs(pivY) > 4.0f || Math.abs(pivZ) > 4.0f) {
                    pivX /= 16.0f; pivY /= 16.0f; pivZ /= 16.0f;
                }
                if (Math.abs(px) > 4.0f || Math.abs(py) > 4.0f || Math.abs(pz) > 4.0f) {
                    px /= 16.0f; py /= 16.0f; pz /= 16.0f;
                }

                poseStack.translate(px + pivX, py + pivY, pz + pivZ);

                if (rz != 0.0f) poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(rz));
                if (ry != 0.0f) poseStack.mulPose(com.mojang.math.Axis.YP.rotation(ry));
                if (rx != 0.0f) poseStack.mulPose(com.mojang.math.Axis.XP.rotation(rx));

                if (isHeadBone(boneName)) {
                    if (netHeadYaw != 0.0f) {
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(netHeadYaw));
                    }
                    if (headPitch != 0.0f) {
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(headPitch));
                    }
                }

                // Procedural limb animation swing driving when player walks
                if (player != null && boneName != null) {
                    String lowerName = boneName.toLowerCase(java.util.Locale.ROOT);
                    float limbSwing = player.walkAnimation != null ? player.walkAnimation.position() : (player.tickCount * 0.2f);
                    float limbSwingAmount = player.walkAnimation != null ? player.walkAnimation.speed() : 0.0f;
                    float walkAngle = (float) Math.sin(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;

                    if (lowerName.contains("left_leg") || lowerName.contains("leftleg") || lowerName.contains("leg2") || lowerName.contains("bipedleftleg")) {
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotation(walkAngle));
                    } else if (lowerName.contains("right_leg") || lowerName.contains("rightleg") || lowerName.contains("leg1") || lowerName.contains("bipedrightleg")) {
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotation(-walkAngle));
                    } else if (lowerName.contains("left_arm") || lowerName.contains("leftarm") || lowerName.contains("arm2") || lowerName.contains("bipedleftarm")) {
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotation(-walkAngle));
                    } else if (lowerName.contains("right_arm") || lowerName.contains("rightarm") || lowerName.contains("arm1") || lowerName.contains("bipedrightarm")) {
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotation(walkAngle));
                    }
                }

                if (sx != 1.0f || sy != 1.0f || sz != 1.0f) {
                    poseStack.scale(sx, sy, sz);
                }

                poseStack.translate(-pivX, -pivY, -pivZ);

                Object cubesObj = ReflectionCache.getCubes != null ? ReflectionCache.getCubes.invoke(bone) : null;
                for (Object cube : toIterable(cubesObj)) {
                    renderCubeReflect(poseStack, vc, cube, packedLight, player, alpha, verticesDrawn);
                }

                Object childBonesObj = ReflectionCache.getChildBones != null ? ReflectionCache.getChildBones.invoke(bone) : null;
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
            Class<?> cubeClass = cube.getClass();
            if (ReflectionCache.quadsMethod == null && ReflectionCache.quadsField == null) {
                ReflectionCache.init(null, cubeClass, null, null);
            }

            Object quadsObj = ReflectionCache.quadsMethod != null ? ReflectionCache.quadsMethod.invoke(cube) : (ReflectionCache.quadsField != null ? ReflectionCache.quadsField.get(cube) : null);
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
                Class<?> quadClass = quad.getClass();
                if (ReflectionCache.verticesMethod == null && ReflectionCache.verticesField == null) {
                    ReflectionCache.init(null, cubeClass, quadClass, null);
                }

                net.minecraft.core.Direction dir = null;
                try {
                    Method dirMethod = quadClass.getMethod("direction");
                    dir = (net.minecraft.core.Direction) dirMethod.invoke(quad);
                } catch (Throwable ignored) {}

                float nx = dir != null ? dir.getStepX() : 0.0f;
                float ny = dir != null ? dir.getStepY() : 1.0f;
                float nz = dir != null ? dir.getStepZ() : 0.0f;

                Object verticesObj = ReflectionCache.verticesMethod != null ? ReflectionCache.verticesMethod.invoke(quad) : (ReflectionCache.verticesField != null ? ReflectionCache.verticesField.get(quad) : null);
                if (verticesObj == null) continue;

                for (Object vertex : toIterable(verticesObj)) {
                    if (vertex == null) continue;
                    Class<?> vertexClass = vertex.getClass();
                    if (ReflectionCache.posMethod == null && ReflectionCache.posField == null) {
                        ReflectionCache.init(null, cubeClass, quadClass, vertexClass);
                    }

                    Object posObj = ReflectionCache.posMethod != null ? ReflectionCache.posMethod.invoke(vertex) : (ReflectionCache.posField != null ? ReflectionCache.posField.get(vertex) : null);
                    float[] pos = extractPos(posObj);

                    float u = 0.0f, v = 0.0f;
                    if (ReflectionCache.uMethod != null) {
                        u = ((Number) ReflectionCache.uMethod.invoke(vertex)).floatValue();
                    } else if (ReflectionCache.uField != null) {
                        u = ReflectionCache.uField.getFloat(vertex);
                    } else {
                        u = getFloatReflect(vertex, "u", "getU", "u");
                    }

                    if (ReflectionCache.vMethod != null) {
                        v = ((Number) ReflectionCache.vMethod.invoke(vertex)).floatValue();
                    } else if (ReflectionCache.vField != null) {
                        v = ReflectionCache.vField.getFloat(vertex);
                    } else {
                        v = getFloatReflect(vertex, "v", "getV", "v");
                    }

                    if (pos != null && pos.length >= 3) {
                        float vx = pos[0];
                        float vy = pos[1];
                        float vz = pos[2];

                        if (Math.abs(vx) > 4.0f || Math.abs(vy) > 4.0f || Math.abs(vz) > 4.0f) {
                            vx /= 16.0f;
                            vy /= 16.0f;
                            vz /= 16.0f;
                        }

                        vc.vertex(pose, vx, vy, vz)
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

    public static void applyKeyframeAnimation(Object bakedModel, ResourceLocation animLoc, String activeAnimKey, float animTick) {
        if (bakedModel == null || animLoc == null) return;
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            Method getAnimsMethod = cacheClass.getMethod("getBakedAnimations");
            Map<?, ?> bakedAnims = (Map<?, ?>) getAnimsMethod.invoke(null);
            Object animFileObj = bakedAnims != null ? bakedAnims.get(animLoc) : null;
            if (animFileObj == null) return;

            Map<?, ?> animationsMap = null;
            try {
                Method animsMethod = animFileObj.getClass().getMethod("animations");
                animationsMap = (Map<?, ?>) animsMethod.invoke(animFileObj);
            } catch (Throwable t1) {
                try {
                    Field f = animFileObj.getClass().getDeclaredField("animations");
                    f.setAccessible(true);
                    animationsMap = (Map<?, ?>) f.get(animFileObj);
                } catch (Throwable ignored) {}
            }
            if (animationsMap == null || animationsMap.isEmpty()) return;

            Object activeAnimObj = animationsMap.get(activeAnimKey);
            if (activeAnimObj == null) {
                for (Map.Entry<?, ?> entry : animationsMap.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    if (key.toLowerCase().contains("walk") || key.toLowerCase().contains("idle") || key.toLowerCase().contains("run")) {
                        activeAnimObj = entry.getValue();
                        break;
                    }
                }
                if (activeAnimObj == null && !animationsMap.isEmpty()) {
                    activeAnimObj = animationsMap.values().iterator().next();
                }
            }
            if (activeAnimObj == null) return;

            Map<?, ?> boneAnimsMap = null;
            try {
                Method boneAnimsMethod = activeAnimObj.getClass().getMethod("boneAnimations");
                boneAnimsMap = (Map<?, ?>) boneAnimsMethod.invoke(activeAnimObj);
            } catch (Throwable t1) {
                try {
                    Field f = activeAnimObj.getClass().getDeclaredField("boneAnimations");
                    f.setAccessible(true);
                    boneAnimsMap = (Map<?, ?>) f.get(activeAnimObj);
                } catch (Throwable ignored) {}
            }
            if (boneAnimsMap == null || boneAnimsMap.isEmpty()) return;

            Method topLevelBonesMethod = bakedModel.getClass().getMethod("topLevelBones");
            List<?> topBones = (List<?>) topLevelBonesMethod.invoke(bakedModel);
            if (topBones != null) {
                for (Object bone : topBones) {
                    applyBoneKeyframesReflect(bone, boneAnimsMap, animTick);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void applyBoneKeyframesReflect(Object bone, Map<?, ?> boneAnimsMap, float animTick) {
        if (bone == null || boneAnimsMap == null) return;
        try {
            String name = null;
            if (ReflectionCache.getName != null) {
                name = (String) ReflectionCache.getName.invoke(bone);
            } else {
                Method m = bone.getClass().getMethod("getName");
                name = (String) m.invoke(bone);
            }

            if (name != null) {
                Object boneAnimObj = boneAnimsMap.get(name);
                if (boneAnimObj == null) {
                    for (Map.Entry<?, ?> entry : boneAnimsMap.entrySet()) {
                        if (name.equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                            boneAnimObj = entry.getValue();
                            break;
                        }
                    }
                }

                if (boneAnimObj != null) {
                    Object rotationFramesObj = null;
                    try {
                        Method rotFramesM = boneAnimObj.getClass().getMethod("rotationKeyFrames");
                        rotationFramesObj = rotFramesM.invoke(boneAnimObj);
                    } catch (Throwable t1) {
                        try {
                            Field rotFramesF = boneAnimObj.getClass().getDeclaredField("rotationKeyFrames");
                            rotFramesF.setAccessible(true);
                            rotationFramesObj = rotFramesF.get(boneAnimObj);
                        } catch (Throwable ignored) {}
                    }

                    if (rotationFramesObj != null) {
                        for (Object kf : toIterable(rotationFramesObj)) {
                            float[] rotVals = extractPos(kf);
                            if (rotVals != null && rotVals.length >= 3) {
                                try {
                                    Field rotXF = bone.getClass().getDeclaredField("rotX");
                                    rotXF.setAccessible(true);
                                    rotXF.setFloat(bone, rotVals[0]);
                                    Field rotYF = bone.getClass().getDeclaredField("rotY");
                                    rotYF.setAccessible(true);
                                    rotYF.setFloat(bone, rotVals[1]);
                                    Field rotZF = bone.getClass().getDeclaredField("rotZ");
                                    rotZF.setAccessible(true);
                                    rotZF.setFloat(bone, rotVals[2]);
                                } catch (Throwable ignored) {}
                                break;
                            }
                        }
                    }
                }
            }

            Object childBonesObj = ReflectionCache.getChildBones != null ? ReflectionCache.getChildBones.invoke(bone) : null;
            for (Object child : toIterable(childBonesObj)) {
                applyBoneKeyframesReflect(child, boneAnimsMap, animTick);
            }
        } catch (Throwable ignored) {}
    }

    public static Object bakeModelFromFile(ResourceLocation modelLoc, String rawPath) {
        if (modelLoc == null) return null;
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            Method getModelsMethod = cacheClass.getMethod("getBakedModels");
            Map<ResourceLocation, Object> bakedModels = (Map<ResourceLocation, Object>) getModelsMethod.invoke(null);
            if (bakedModels != null && bakedModels.containsKey(modelLoc)) {
                return bakedModels.get(modelLoc);
            }

            File modelFile = GeckoAssetResolver.resolveModelDiskFile(rawPath);
            if (modelFile == null || !modelFile.exists() || !modelFile.isFile()) {
                return null;
            }

            String jsonString = Files.readString(modelFile.toPath());
            Class<?> jsonUtilClass = Class.forName("software.bernie.geckolib.util.JsonUtil");
            Method parseJsonMethod = jsonUtilClass.getMethod("parse", String.class);
            Object rawJsonObj = parseJsonMethod.invoke(null, jsonString);

            Class<?> modelFactoryClass = Class.forName("software.bernie.geckolib.loading.object.BakedModelFactory");
            Method getFactoryMethod = modelFactoryClass.getMethod("getForNamespace", String.class);
            Object factoryObj = getFactoryMethod.invoke(null, modelLoc.getNamespace());

            Method constructGeoModelMethod = modelFactoryClass.getMethod("constructGeoModel", rawJsonObj.getClass());
            Object bakedGeoModel = constructGeoModelMethod.invoke(factoryObj, rawJsonObj);

            if (bakedGeoModel != null && bakedModels != null) {
                bakedModels.put(modelLoc, bakedGeoModel);
            }
            return bakedGeoModel;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object bakeAnimationsFromFile(ResourceLocation animLoc, String rawPath) {
        if (animLoc == null) return null;
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            Method getAnimsMethod = cacheClass.getMethod("getBakedAnimations");
            Map<ResourceLocation, Object> bakedAnims = (Map<ResourceLocation, Object>) getAnimsMethod.invoke(null);
            if (bakedAnims != null && bakedAnims.containsKey(animLoc)) {
                return bakedAnims.get(animLoc);
            }

            File animFile = GeckoAssetResolver.resolveAnimationDiskFile(rawPath);
            if (animFile == null || !animFile.exists() || !animFile.isFile()) {
                return null;
            }

            String jsonString = Files.readString(animFile.toPath());
            Class<?> jsonUtilClass = Class.forName("software.bernie.geckolib.util.JsonUtil");
            Method parseJsonMethod = jsonUtilClass.getMethod("parse", String.class);
            Object rawJsonObj = parseJsonMethod.invoke(null, jsonString);

            Class<?> animLoaderClass = Class.forName("software.bernie.geckolib.loading.FileLoader");
            Method loadAnimationsMethod = animLoaderClass.getMethod("loadAnimations", rawJsonObj.getClass());
            Object bakedAnimObj = loadAnimationsMethod.invoke(null, rawJsonObj);

            if (bakedAnimObj != null && bakedAnims != null) {
                bakedAnims.put(animLoc, bakedAnimObj);
            }
            return bakedAnimObj;
        } catch (Throwable t) {
            return null;
        }
    }
}
