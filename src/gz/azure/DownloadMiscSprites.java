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
import java.nio.file.Path;
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

    public DownloadMiscSprites(File file, Boolean stripClass) {
        this.file = file;
        this.stripClass = stripClass;
        this.className = this.file.getName().replace(".swf", "");
    }

    @Override
    public void run() {
        String path;
        String swfpath;
        if (OSCheck.getOperatingSystemType() == OSCheck.OSType.Windows) {
            path = "camera\\gordon\\";
            swfpath = "swf\\" + Itemtype + "\\";
        }
        else {
            //path = imageType + "/" + Itemtype + "/";
            path = "camera/gordon/";
            swfpath = "swf/" + Itemtype + "/";
        }

        try {
            if (!Files.isDirectory(Paths.get(path))) Files.createDirectory(Paths.get(path));
            //if (!Files.isDirectory(Paths.get(swfpath))) Files.createDirectory(Paths.get(swfpath));
        } catch(java.io.IOException e) {
            Log.error("Unable to create output directory");
        }

        try {
            if(!SpriteExtractor.isLocal) {
                URL swfURL = new URL(SpriteExtractor.externalVariables.getFlashClientURL() + className + ".swf");
                Log.info("Extracting: " + swfURL.toString());

                SWFData swfData = new SWFData(swfURL);

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
                //swfData.getswf().saveTo(new FileOutputStream(swfpath + className + ".swf"));
            }
            else {
                Log.info("Extracting: " + file.getName());
                SWFData swfData = new SWFData(file);

                for (ImageTag tag : swfData.getImageTags()) {
                    String spriteName;
                    if (stripClass) {
                        spriteName = tag.getClassName().replace(className + "_", "");
                        ImageIO.write(tag.getImage().getBufferedImage(), "png", new File(path + spriteName + ".png"));
                    }
                        spriteName = Arrays.stream(tag.getClassName().split(className + "_"))
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
