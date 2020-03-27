package gz.azure;

import com.jpexs.decompiler.flash.tags.Tag;
import com.jpexs.decompiler.flash.tags.base.ImageTag;
import gz.azure.utils.Log;
import gz.azure.utils.OSCheck;
import gz.azure.utils.SWFData;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by Scott on 8/18/2015.
 */
public class DownloadMiscSprites implements Runnable {
    private String imageType;
    private String className;
    private Boolean stripClass;
    private String Itemtype;

    private File file;

    public DownloadMiscSprites(String imageType, String className, Boolean stripClass, String type) {
        this.imageType = imageType;
        this.className = className;
        this.stripClass = stripClass;
        this.Itemtype = type;
    }

    public DownloadMiscSprites(File file, Boolean stripClass, String type) {
        this.file = file;
        this.stripClass = stripClass;
        this.Itemtype = type;
        this.className = this.file.getName().replace(".swf", "");
    }

    @Override
    public void run() {
        String path;
        String savePath;
        String manifestPath;
        if (OSCheck.getOperatingSystemType() == OSCheck.OSType.Windows) {
            path = "output\\" + Itemtype + "\\sprites\\";
            savePath = "output\\swf\\" + Itemtype + "\\";
            manifestPath = "output\\" + Itemtype + "\\manifest\\";
        } else {
            //path = imageType + "/" + Itemtype + "/";
            path = "output/" + Itemtype + "/sprites/";
            savePath = "output/swf/" + Itemtype + "/";
            manifestPath = "output/" + Itemtype + "/manifest/";
        }

        try {
            if (!Files.isDirectory(Paths.get(path))) Files.createDirectories(Paths.get(path));
            if (!Files.isDirectory(Paths.get(manifestPath))) Files.createDirectories(Paths.get(manifestPath));
            if (SpriteExtractor.saveDownloads && !Files.isDirectory(Paths.get(savePath))) Files.createDirectories(Paths.get(savePath));
        } catch (java.io.IOException e) {
            Log.error("Unable to create output directory");
            Log.error(e.getMessage());
            System.exit(1);
        }

        try {
            SWFData swfData;
            if (!SpriteExtractor.isLocal) {
                URL swfURL = new URL(SpriteExtractor.externalVariables.getFlashClientURL() + className + ".swf");
                Log.info("Extracting: " + swfURL.toString());

                swfData = new SWFData(swfURL);
            } else {
                Log.info("Extracting: " + file.getName());
                swfData = new SWFData(file);
            }
            for (ImageTag tag : swfData.getImageTags()) {
                String spriteName;
                if (stripClass) {
                    spriteName = tag.getClassName().replace(className + "_", "");
                } else {
                    spriteName = Arrays.stream(tag.getClassName().split(className + "_"))
                            .distinct().collect(Collectors.joining(className + "_"));
                }
                ImageIO.write(tag.getImage().getBufferedImage(), "png", new File(path + spriteName + ".png"));
            }
            if(SpriteExtractor.saveDownloads)
                swfData.getswf().saveTo(new FileOutputStream(savePath + className + ".swf")); // saving the downloaded swf

            for (Tag tag : swfData.getManifestBinaryDataTags()) {
                String spriteName = tag.toString().split("\\(")[1].split(":")[1].split("\\)")[0].trim();
                try (FileOutputStream stream = new FileOutputStream(manifestPath + spriteName + ".bin")) {
                    byte[] data = Arrays.copyOfRange(tag.getData(), 6, tag.getData().length);
                    stream.write(data);
                }
            }
        } catch (MalformedURLException ex) {
            Log.error("Malformed URL!", ex);
        } catch (Throwable ex) {
            Log.error("Failed to load/extract SWF!", ex);
        }
    }
}
