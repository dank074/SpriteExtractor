package gz.azure.utils;

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.tags.Tag;
import com.jpexs.decompiler.flash.tags.base.ImageTag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Azure Camera Server - Furni Sprite Extractor
 * Written by Scott Stamp (scottstamp851, scott@hypermine.com)
 */

public class SWFData {
    private SWF swf;

    public SWFData(File file) throws IOException, InterruptedException {
        swf = new SWF(new FileInputStream(file), false, false);
    }


    public SWFData(URL fileLoc) throws IOException, InterruptedException {
        HttpURLConnection conn = (HttpURLConnection) fileLoc.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        swf = new SWF(conn.getInputStream(), false, false);
    }

    public List<Tag> getTags() {
        return swf.tags;
    }

    public List<ImageTag> getImageTags() {
        return swf.tags.stream()
                .filter((tag) -> tag.getTagName().equals("DefineBitsLossless2"))
                .map((tag) -> swf.getImage(Integer.parseInt(tag.toString().split("\\(")[1].split(":")[0])))
                .collect(Collectors.toList());
    }

    public SWF getswf() {
        return this.swf;
    }
}
