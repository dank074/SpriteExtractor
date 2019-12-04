package gz.azure;

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

public class DownloadFurniSprites implements Runnable {
    private int revision;
    private String className;

    private File file;

    public DownloadFurniSprites(int revision, String className) {
        this.revision = revision;
        this.className = className;
    }

    public DownloadFurniSprites(File file) {
        this.file = file;
        this.className = this.file.getName().replace(".swf", "");
    }

    @Override
    public void run() {
        String path;
        String savePath;
        if (OSCheck.getOperatingSystemType() == OSCheck.OSType.Windows) {
            path = "output\\furnis\\sprites\\";
            savePath = "output\\swf\\furnis\\";
        } else {
            path = "output/furnis/sprites/";
            savePath = "output/swf/furnis/";
        }

        try {
            if (!Files.isDirectory(Paths.get(path))) Files.createDirectories(Paths.get(path));
            if (SpriteExtractor.saveDownloads && !Files.isDirectory(Paths.get(savePath))) Files.createDirectories(Paths.get(savePath));
        } catch (java.io.IOException e) {
            Log.error("Unable to create directory for output");
            Log.error(e.getMessage());
            System.exit(1);
        }

        try {
            SWFData swfData;
            if (!SpriteExtractor.isLocal) {
                URL swfURL = new URL(SpriteExtractor.externalVariables.getFlashDynamicDownloadURL() + className + ".swf");
                Log.info("Extracting: " + swfURL.toString());
                swfData = new SWFData(swfURL);
            } else {
                Log.info("Extracting: " + file.getName());
                swfData = new SWFData(file);
            }
            for (ImageTag tag : swfData.getImageTags()) {
                String spriteName = Arrays.stream(tag.getClassName().split(className + "_"))
                        .distinct().collect(Collectors.joining(className + "_"));
                ImageIO.write(tag.getImage().getBufferedImage(), "png", new File(path + spriteName + ".png"));
            }
            if(SpriteExtractor.saveDownloads)
                swfData.getswf().saveTo(new FileOutputStream(savePath + className + ".swf"));
        } catch (MalformedURLException ex) {
            Log.error("Malformed URL!", ex);
        } catch (Throwable ex) {
            Log.error("Failed to load/extract SWF!", ex);
        }
    }
}
