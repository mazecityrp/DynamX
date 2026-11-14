package fr.dynamx.client.renders.model.renderer;

import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The renderer of an external model which has not been supplied yet (see {@link fr.dynamx.common.objloader.ExternalModels}). <br>
 * It draws nothing, and reports that everything was drawn: the model is not missing, it is not there yet.
 */
public class PendingObjModelRenderer extends ObjModelRenderer {
    private final List<String> objectNames;

    /**
     * @param objectNames The names of the objects of the model, in lower case, known from its metadata
     */
    public PendingObjModelRenderer(DxModelPath location, List<String> objectNames, @Nullable IModelTextureVariantsSupplier textureVariants) {
        super(location, new ArrayList<>(), new HashMap<>(), textureVariants);
        this.objectNames = objectNames;
    }

    @Override
    public void renderGroup(ObjObjectRenderer obj, byte textureDataId, boolean forceVanillaRender) {
    }

    @Override
    public boolean renderGroup(String group, byte textureDataId, boolean forceVanillaRender) {
        return true;
    }

    @Override
    public boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender) {
        return true;
    }

    @Override
    public void renderModel(byte textureDataId, boolean forceVanillaRender) {
    }

    @Override
    public boolean containsObjectOrNode(String name) {
        return objectNames.contains(name.toLowerCase());
    }

    /**
     * @return False: an empty model is drawn as a loading error
     */
    @Override
    public boolean isEmpty() {
        return false;
    }
}
