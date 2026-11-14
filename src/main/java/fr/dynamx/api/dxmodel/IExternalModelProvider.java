package fr.dynamx.api.dxmodel;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Supplies models that are not stored in the content packs. <br>
 * A model is <i>external</i> when its file is absent from its pack and a metadata file, named after the model with the
 * {@code .dxmeta} suffix, is present instead (see {@link fr.dynamx.common.objloader.ExternalModels}). <br>
 * Until its provider supplies it, an external model is described by its metadata only, and draws nothing.
 *
 * @see fr.dynamx.common.objloader.ExternalModels#setProvider(IExternalModelProvider)
 * @see fr.dynamx.client.DynamXModelRegistry#loadExternalModels(java.util.Collection)
 */
public interface IExternalModelProvider {
    /**
     * Called each time DynamX needs the content of a model, possibly from several threads at the same time
     *
     * @param path The model
     * @return The content of the model file, or null if this provider cannot supply this model (yet). The stream is closed by DynamX.
     */
    @Nullable
    InputStream openModel(DxModelPath path) throws IOException;
}
