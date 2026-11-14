package fr.dynamx.common.objloader;

import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.IExternalModelProvider;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.objloader.data.ExternalObjModelData;
import fr.dynamx.common.objloader.data.ObjModelData;
import fr.dynamx.utils.DynamXUtils;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Support of external models: models whose file is not in the content pack, and that an {@link IExternalModelProvider} may supply later. <br>
 * The pack only contains a metadata file, named {@code <model path>.dxmeta}, giving the name and the bounding box of each
 * object of the model: this is all the pack loader reads from a model, complex collisions excepted (they need a .dc file).
 * <pre>
 * {
 *   "format": 1,
 *   "meshes": [ { "name": "chassis", "min": [-1.3, -0.5, -2.8], "max": [1.3, 1.2, 2.9] } ]
 * }
 * </pre>
 */
public final class ExternalModels {
    /**
     * Suffix appended to the path of a model to get the path of its metadata file
     */
    public static final String METADATA_SUFFIX = ".dxmeta";

    private static volatile IExternalModelProvider provider;

    private ExternalModels() {
    }

    /**
     * Sets the provider of the external models. Can be called at any time, from any thread.
     *
     * @param provider The provider, or null to remove it
     */
    public static void setProvider(@Nullable IExternalModelProvider provider) {
        ExternalModels.provider = provider;
    }

    @Nullable
    public static IExternalModelProvider getProvider() {
        return provider;
    }

    /**
     * @return The content of the model, if a provider can supply it now, null otherwise
     */
    @Nullable
    public static InputStream open(DxModelPath path) {
        IExternalModelProvider current = provider;
        if (current == null) {
            return null;
        }
        try {
            return current.openModel(path);
        } catch (Exception e) {
            DynamXMain.log.error("The external model provider failed to supply " + path, e);
            return null;
        }
    }

    /**
     * Reads the data of an obj model: from the provider if it supplies the model, from the pack otherwise. <br>
     * If the model file is absent from the pack and has a metadata file, the returned data is built from the metadata.
     *
     * @throws RuntimeException If the model cannot be read
     */
    public static ObjModelData loadObjModelData(DxModelPath path) {
        InputStream supplied = open(path);
        if (supplied != null) {
            return new ObjModelData(path, supplied);
        }
        byte[] metadata = readMetadata(path);
        if (metadata != null && !isInPacks(path)) {
            try {
                return new ExternalObjModelData(path, metadata);
            } catch (Exception e) {
                throw new RuntimeException("Metadata of the external model " + path + " cannot be read ! " + e, e);
            }
        }
        return new ObjModelData(path);
    }

    /**
     * @return The content of the metadata file of this model, or null if it has none
     */
    @Nullable
    public static byte[] readMetadata(DxModelPath path) {
        ResourceLocation model = path.getModelPath();
        return read(path, new ResourceLocation(model.getNamespace(), model.getPath() + METADATA_SUFFIX));
    }

    /**
     * @return True if the model file is in one of the packs of this path
     */
    public static boolean isInPacks(DxModelPath path) {
        for (PackInfo packInfo : path.getPackLocations()) {
            try {
                InputStream stream = packInfo.readFile(path.getModelPath());
                if (stream != null) {
                    stream.close();
                    return true;
                }
            } catch (IOException ignored) {
            }
        }
        return false;
    }

    @Nullable
    private static byte[] read(DxModelPath path, ResourceLocation file) {
        for (PackInfo packInfo : path.getPackLocations()) {
            try {
                InputStream stream = packInfo.readFile(file);
                if (stream != null) {
                    return DynamXUtils.readInputStream(stream);
                }
            } catch (IOException e) {
                DynamXMain.log.warn("Cannot read " + file + " in " + packInfo.getPathName(), e);
            }
        }
        return null;
    }
}
