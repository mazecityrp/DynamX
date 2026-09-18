package fr.dynamx.utils;

import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;

public class DynamXConstants {
    public static final String NAME = "DynamX";
    public static final String ID = "dynamxmod";
    public static final String VERSION = "4.1.0";
    public static final String VERSION_TYPE = "dev69";
    public static final String RES_DIR_NAME = "DynamX";
    /**
     * Side-specific content pack folders: "DxClient" on clients, "DxServer" on servers. This matches the official DynamX
     * 4.1.0-dev69 build distributed on files.dynamx.fr (its DynamXMain uses these names), which the GitHub sources never got.
     * RES_DIR_NAME is kept for compatibility with older code but is no longer used to load packs.
     */
    public static final String CLIENT_RES_DIR_NAME = "DxClient", SERVER_RES_DIR_NAME = "DxServer";

    public static final String ACS_GUIS_BASE_URL = "https://maven.dynamx.fr/artifactory/ACsGuisRepo/fr/aym/acsguis/ACsGuis/%1$s/ACsGuis-%1$s.jar";//"https://mps.dynamx.fr/files/4.0.0/ACsGuis/ACsGuis-%s-all.jar";
    public static final String DEFAULT_ACSGUIS_VERSION = "1.4.1-beta";
    public static final String ACSGUIS_REQUIRED_VERSION = "[1.4.0,)";
    public static final String ACSLIBS_REQUIRED_VERSION = "[1.2.12,)";

    public static final String LIBBULLET_BASE_URL = "https://mps.dynamx.fr/files/4.0.0/LibBullet/";

    public static final String LIBBULLET_VERSION = "18.1.0";
    /**
     * .dc file version, only change when an update of libbullet breaks the .dc files, to regenerate them
     */
    public static final String DC_FILE_VERSION = "12.5.0";
    /**
     * Version of the {@link fr.dynamx.common.contentpack.ContentPackLoader}
     */
    public static final ArtifactVersion PACK_LOADER_VERSION = new DefaultArtifactVersion("1.1.0");

    public static final String DYNAMX_CERT = "certs/letsencrypt-isrgrootx1.der", DYNAMX_AUX_CERT = "certs/DynamXRootCA.crt";

    public static final String MPS_URL = "https://mps.dynamx.fr/", MPS_KEY = "VGZQNFY5UEJ1YmVLTlpwWC1BQ1JFLTQ=", MPS_STARTER = null;
    public static final String[] OLD_MPS_URLS = new String[]{"https://dynamx.fr/mps/", "https://mps.dynamx.fr/legacy/"};
    public static final String[] MPS_AUX_URLS = new String[]{"https://mps2.dynamx.fr/"};

    public static final String STATS_URL = "https://dynamx.fr/statsbot/statsbotrcv.php", STATS_PRODUCT = "DNX_" + VERSION + "_BETA", STATS_TOKEN = "ZG54OnN0YWJkOTg=";

    public static final boolean REMAP = true;
}