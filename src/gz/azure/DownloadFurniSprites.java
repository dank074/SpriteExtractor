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
        String swfpath;
        if (OSCheck.getOperatingSystemType() == OSCheck.OSType.Windows) {
            path = "camera\\furnis\\";
            swfpath = "swf\\furnis\\";
        } else {
            path = "camera/furnis";
            swfpath = "swf/furnis";
        }

        try {
            if (!Files.isDirectory(Paths.get(path))) Files.createDirectory(Paths.get(path));
            //if (!Files.isDirectory(Paths.get(swfpath))) Files.createDirectory(Paths.get(swfpath));
        } catch(java.io.IOException e){
            Log.error("Unable to create directory for output");
        }

        try {
            if(!SpriteExtractor.isLocal) {
                URL swfURL = new URL(SpriteExtractor.externalVariables.getFlashDynamicDownloadURL() + className + ".swf");
                Log.info("Extracting: " + swfURL.toString());

                SWFData swfData = new SWFData(swfURL);

                for (ImageTag tag : swfData.getImageTags()) {
                    String spriteName = Arrays.stream(tag.getClassName().split(className + "_"))
                            .distinct().collect(Collectors.joining(className + "_"));
                    ImageIO.write(tag.getImage().getBufferedImage(), "png", new File(path + spriteName + ".png"));
                }
                //swfData.getswf().saveTo(new FileOutputStream(swfpath + className + ".swf"));
            }
            else {
                Log.info("Extracting: " + file.getName());
                SWFData swfData = new SWFData(file);

                for (ImageTag tag : swfData.getImageTags()) {
                    String spriteName = Arrays.stream(tag.getClassName().split(className + "_"))
                            .distinct().collect(Collectors.joining(className + "_"));
                    ImageIO.write(tag.getImage().getBufferedImage(), "png", new File(path + spriteName + ".png"));
                }
            }
        } catch (MalformedURLException ex) {
            Log.error("Malformed URL!", ex);
        } catch (Throwable ex) {
            Log.error("Failed to load/extract SWF!", ex);
        }
    }
}
