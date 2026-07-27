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
import java.util.List;
import java.util.Map;

/**
 * Robust, reflection-backed GeckoLib model loader and bone renderer for Custom Races.
 * Dynamically loads and renders ANY GeckoLib .geo.json model (bears, cats, dragons, beasts, etc.)
 * safely across Fabric and Forge environments without hardcoded mesh assumptions.
 */
public class GeckoLibWereRenderer {

    public static boolean isModelPresent(ResourceLocation modelLoc) {
        if (modelLoc == null) return false;
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            Method getModelsMethod = cacheClass.getMethod("getBakedModels");
            Map<?, ?> bakedModels = (Map<?, ?>) getModelsMethod.invoke(null);
            if (bakedModels != null && bakedModels.containsKey(modelLoc)) {
                return true;
            }
            return bakeModelFromFile(modelLoc) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean renderGeckoModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, RaceData race, ResourceLocation modelLoc, ResourceLocation textureLoc, ResourceLocation animLoc) {
        if (modelLoc == null) return false;
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            Method getModelsMethod = cacheClass.getMethod("getBakedModels");
            Map<?, ?> bakedModels = (Map<?, ?>) getModelsMethod.invoke(null);
            
            Object bakedModel = bakedModels != null ? bakedModels.get(modelLoc) : null;
            if (bakedModel == null) {
                bakedModel = bakeModelFromFile(modelLoc);
            }
            if (bakedModel == null) return false;

            Method topLevelBonesMethod = bakedModel.getClass().getMethod("topLevelBones");
            List<?> topBones = (List<?>) topLevelBonesMethod.invoke(bakedModel);
            if (topBones == null || topBones.isEmpty()) return false;

            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(textureLoc));

            poseStack.pushPose();
            try {
                // Align GeckoLib model origin to entity feet (0.0, 0.0, 0.0)
                for (Object bone : topBones) {
                    renderBoneReflect(poseStack, vc, bone, packedLight, player);
                }
            } finally {
                poseStack.popPose();
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void renderBoneReflect(PoseStack poseStack, VertexConsumer vc, Object bone, int packedLight, AbstractClientPlayer player) {
        if (bone == null) return;
        try {
            Method isHiddenMethod = bone.getClass().getMethod("isHidden");
            if ((Boolean) isHiddenMethod.invoke(bone)) return;

            Method getPivotX = bone.getClass().getMethod("getPivotX");
            Method getPivotY = bone.getClass().getMethod("getPivotY");
            Method getPivotZ = bone.getClass().getMethod("getPivotZ");

            Method getPosX = bone.getClass().getMethod("getPosX");
            Method getPosY = bone.getClass().getMethod("getPosY");
            Method getPosZ = bone.getClass().getMethod("getPosZ");

            Method getRotX = bone.getClass().getMethod("getRotX");
            Method getRotY = bone.getClass().getMethod("getRotY");
            Method getRotZ = bone.getClass().getMethod("getRotZ");

            Method getScaleX = bone.getClass().getMethod("getScaleX");
            Method getScaleY = bone.getClass().getMethod("getScaleY");
            Method getScaleZ = bone.getClass().getMethod("getScaleZ");

            float pivX = (Float) getPivotX.invoke(bone);
            float pivY = (Float) getPivotY.invoke(bone);
            float pivZ = (Float) getPivotZ.invoke(bone);

            float px = (Float) getPosX.invoke(bone);
            float py = (Float) getPosY.invoke(bone);
            float pz = (Float) getPosZ.invoke(bone);

            float rx = (Float) getRotX.invoke(bone);
            float ry = (Float) getRotY.invoke(bone);
            float rz = (Float) getRotZ.invoke(bone);

            float sx = (Float) getScaleX.invoke(bone);
            float sy = (Float) getScaleY.invoke(bone);
            float sz = (Float) getScaleZ.invoke(bone);

            poseStack.pushPose();
            try {
                // 1. Move to bone position + pivot origin
                poseStack.translate((px + pivX) / 16.0f, (py + pivY) / 16.0f, (pz + pivZ) / 16.0f);

                // 2. Apply Euler rotations around joint pivot
                if (rz != 0.0f) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-rz));
                if (ry != 0.0f) poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-ry));
                if (rx != 0.0f) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rx));

                // 3. Apply bone scaling
                if (sx != 1.0f || sy != 1.0f || sz != 1.0f) {
                    poseStack.scale(sx, sy, sz);
                }

                // 4. Translate back by pivot offset
                poseStack.translate(-pivX / 16.0f, -pivY / 16.0f, -pivZ / 16.0f);

                Method getCubes = bone.getClass().getMethod("getCubes");
                List<?> cubes = (List<?>) getCubes.invoke(bone);
                if (cubes != null) {
                    for (Object cube : cubes) {
                        renderCubeReflect(poseStack, vc, cube, packedLight, player);
                    }
                }

                Method getChildBones = bone.getClass().getMethod("getChildBones");
                List<?> childBones = (List<?>) getChildBones.invoke(bone);
                if (childBones != null) {
                    for (Object child : childBones) {
                        renderBoneReflect(poseStack, vc, child, packedLight, player);
                    }
                }
            } finally {
                poseStack.popPose();
            }
        } catch (Throwable ignored) {}
    }

    private static void renderCubeReflect(PoseStack poseStack, VertexConsumer vc, Object cube, int packedLight, AbstractClientPlayer player) {
        if (cube == null) return;
        try {
            Object[] quads = null;
            try {
                Method quadsMethod = cube.getClass().getMethod("quads");
                quads = (Object[]) quadsMethod.invoke(cube);
            } catch (Throwable ignored) {
                Field quadsField = cube.getClass().getDeclaredField("quads");
                quadsField.setAccessible(true);
                quads = (Object[]) quadsField.get(cube);
            }

            if (quads == null) return;
            org.joml.Matrix4f pose = poseStack.last().pose();
            org.joml.Matrix3f normal = poseStack.last().normal();
            int overlay = (player != null && player.hurtTime > 0) ? OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(true)) : OverlayTexture.NO_OVERLAY;

            for (Object quad : quads) {
                if (quad == null) continue;
                net.minecraft.core.Direction dir = null;
                try {
                    Method dirMethod = quad.getClass().getMethod("direction");
                    dir = (net.minecraft.core.Direction) dirMethod.invoke(quad);
                } catch (Throwable ignored) {}

                float nx = dir != null ? dir.getStepX() : 0.0f;
                float ny = dir != null ? dir.getStepY() : 1.0f;
                float nz = dir != null ? dir.getStepZ() : 0.0f;

                Object[] vertices = null;
                try {
                    Method verticesMethod = quad.getClass().getMethod("vertices");
                    vertices = (Object[]) verticesMethod.invoke(quad);
                } catch (Throwable ignored) {
                    Field verticesField = quad.getClass().getDeclaredField("vertices");
                    verticesField.setAccessible(true);
                    vertices = (Object[]) verticesField.get(quad);
                }

                if (vertices == null) continue;
                for (Object vertex : vertices) {
                    if (vertex == null) continue;
                    org.joml.Vector3f pos = null;
                    try {
                        Method posMethod = vertex.getClass().getMethod("position");
                        pos = (org.joml.Vector3f) posMethod.invoke(vertex);
                    } catch (Throwable ignored) {
                        Field posField = vertex.getClass().getDeclaredField("position");
                        posField.setAccessible(true);
                        pos = (org.joml.Vector3f) posField.get(vertex);
                    }

                    float u = 0.0f, v = 0.0f;
                    try {
                        Method uMethod = vertex.getClass().getMethod("u");
                        u = (Float) uMethod.invoke(vertex);
                        Method vMethod = vertex.getClass().getMethod("v");
                        v = (Float) vMethod.invoke(vertex);
                    } catch (Throwable ignored) {
                        try {
                            Field uField = vertex.getClass().getDeclaredField("u");
                            uField.setAccessible(true);
                            u = uField.getFloat(vertex);
                            Field vField = vertex.getClass().getDeclaredField("v");
                            vField.setAccessible(true);
                            v = vField.getFloat(vertex);
                        } catch (Throwable ignored2) {}
                    }

                    if (pos != null) {
                        vc.vertex(pose, pos.x() / 16.0f, pos.y() / 16.0f, pos.z() / 16.0f)
                                .color(1.0f, 1.0f, 1.0f, 1.0f)
                                .uv(u, v)
                                .overlayCoords(overlay)
                                .uv2(packedLight)
                                .normal(normal, nx, ny, nz)
                                .endVertex();
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static Object bakeModelFromFile(ResourceLocation modelLoc) {
        if (modelLoc == null) return null;
        try {
            String content = null;
            String cleanPath = modelLoc.getPath();
            File file = new File(cleanPath);
            if (!file.exists()) {
                file = new File("config/custom_races/models/" + cleanPath.replaceAll(".*/", ""));
            }
            if (!file.exists()) {
                file = new File("config/custom_races/models/were/" + cleanPath.replaceAll(".*/", ""));
            }

            if (file.exists() && file.isFile()) {
                content = Files.readString(file.toPath());
            } else {
                try {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc != null && mc.getResourceManager() != null) {
                        var resOpt = mc.getResourceManager().getResource(modelLoc);
                        if (resOpt.isPresent()) {
                            try (java.io.InputStream is = resOpt.get().open();
                                 java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                                content = br.lines().collect(java.util.stream.Collectors.joining("\n"));
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

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

    public static Object bakeAnimationsFromFile(ResourceLocation animLoc) {
        if (animLoc == null) return null;
        try {
            String content = null;
            String cleanPath = animLoc.getPath();
            File file = new File(cleanPath);
            if (!file.exists()) {
                file = new File("config/custom_races/animations/" + cleanPath.replaceAll(".*/", ""));
            }
            if (!file.exists()) {
                file = new File("config/custom_races/animations/were/" + cleanPath.replaceAll(".*/", ""));
            }

            if (file.exists() && file.isFile()) {
                content = Files.readString(file.toPath());
            } else {
                try {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc != null && mc.getResourceManager() != null) {
                        var resOpt = mc.getResourceManager().getResource(animLoc);
                        if (resOpt.isPresent()) {
                            try (java.io.InputStream is = resOpt.get().open();
                                 java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                                content = br.lines().collect(java.util.stream.Collectors.joining("\n"));
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

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
}
