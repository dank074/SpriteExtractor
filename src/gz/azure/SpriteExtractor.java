package gz.azure;

import gz.azure.txt.ExternalVariables;
import gz.azure.utils.Log;
import gz.azure.utils.OSCheck;
import gz.azure.xml.effectmap.Effectmap;
import gz.azure.xml.effectmap.EffectmapParser;
import gz.azure.xml.figuremap.Figuremap;
import gz.azure.xml.figuremap.FiguremapParser;
import gz.azure.xml.furnidata.Furnidata;
import gz.azure.xml.furnidata.FurnidataParser;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.cli.*;

/**
 * Azure Camera Server - Furni Sprite Extractor
 * Written by Scott Stamp (scottstamp851, scott@hypermine.com)
 */
public class SpriteExtractor {
    public static ExternalVariables externalVariables;
    private static boolean extractMisc = false;
    private static boolean extractMasks = false;
    private static boolean extractPets =false;
    private static boolean extractFigures = false;
    private static boolean extractEffects =false;
    private static boolean extractFurni = false;
    public static boolean isLocal = false;
    public static boolean saveDownloads = false;


    public static void main(String[] args) throws Throwable {
        Log.println("               Azure Camera Server - Sprite Extractor");
        Log.println("      Written by Scott Stamp (scottstamp851, scott@hypermine.com)");
        Log.println();
        Log.println("Note: Some files may fail to download/extract. Common with low revision ID numbers.");
        Log.println("      It means they're no longer hosted on Habbo's server. This is normal.");
        Log.println();
        ExecutorService downloadPool = Executors.newFixedThreadPool(8);
        List<Callable<Object>> downloadTasks = new ArrayList<>();
        Options options = new Options();

        Option variableUrl = new Option("r", "remote", true, "external variables url");
        variableUrl.setRequired(false);
        variableUrl.setArgName("url");
        options.addOption(variableUrl);

        options.addOption("l", "local", false, "use local directory files");
        options.addOption("c", "clothes", false, "extract clothes sprites");
        options.addOption("f", "furnis", false, "extract furni sprites");
        options.addOption("e", "effects", false, "extract effect sprites");
        options.addOption("p", "pets", false, "extract pet sprites");
        options.addOption("m", "miscellaneous", false, "extract miscellaneous sprites found in config_habbo");
        options.addOption("k", "masks", false, "extract wall/floor/background masks sprites");
        options.addOption("s", "save", false, "save downloaded files if using remote");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("SpriteExtractor", options);
            System.exit(1);
        }

        // Let's get command line arguments

        if(cmd.hasOption("remote")) {
            try {
                externalVariables = new ExternalVariables(new URL(cmd.getOptionValue("remote")));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        else if(cmd.hasOption("local")) {
            isLocal = true;
        }
        else {
            System.out.println("You must specify either a local directory path or a remote url");
            formatter.printHelp("SpriteExtractor", options);
            System.exit(1);
        }
        extractPets = cmd.hasOption("pets");
        extractEffects = cmd.hasOption("effects");
        extractFigures = cmd.hasOption("clothes");
        extractFurni = cmd.hasOption("furnis");
        extractMasks = cmd.hasOption("masks");
        extractMisc = cmd.hasOption("miscellaneous");
        saveDownloads = cmd.hasOption("save");

        // Download misc. files (specified in config_habbo.xml)
        if (extractMisc) downloadMisc(downloadTasks);
        // Download all pet sprites
        if (extractPets) downloadPets(downloadTasks);
        // Download all figure sprites
        if (extractFigures) downloadFigures(downloadTasks);
        // Download all effects sprites
        if (extractEffects) downloadEffects(downloadTasks);
        // Download all wall/floor/landscape masks
        if (extractMasks) downloadMasks(downloadTasks);
        // Download all furni sprites
        if (extractFurni) downloadFurni(downloadTasks);

        downloadPool.invokeAll(downloadTasks);
        downloadPool.shutdown();
        downloadPool.awaitTermination(10, TimeUnit.SECONDS);
    }

    private static void downloadFurni(List<Callable<Object>> downloadTasks) {
        if(!isLocal) {
            Furnidata furnidata = FurnidataParser.getFurnidata(externalVariables.getFurniData());
            if (furnidata != null) {
                List<Furnidata.Roomitemtypes.Furnitype> roomItemTypes
                        = furnidata.getRoomitemtypes().getFurnitype();
                List<Furnidata.Wallitemtypes.Furnitype> wallItemTypes
                        = furnidata.getWallitemtypes().getFurnitype();

                List<String> itemClassNames = new ArrayList<>();

                for (Furnidata.Roomitemtypes.Furnitype item : roomItemTypes) {
                    String className = item.getClassname().split("\\*")[0];
                    if (!itemClassNames.contains(className))
                        downloadTasks.add(Executors.callable(new DownloadFurniSprites(item.getRevision(), className)));

                    itemClassNames.add(className);
                }

                for (Furnidata.Wallitemtypes.Furnitype item : wallItemTypes) {
                    String className = item.getClassname().split("\\*")[0];
                    if (!itemClassNames.contains(className))
                        downloadTasks.add(Executors.callable(new DownloadFurniSprites(item.getRevision(), className)));

                    itemClassNames.add(className);
                }
            }
        }
        else {
            File inFileOrFolder = new File("input\\furnis\\");
            File[] inFiles;
            boolean singleFile = true;
            if (inFileOrFolder.isDirectory()) {
                singleFile = false;
                inFiles = inFileOrFolder.listFiles(OSCheck.getSwfFilter());
            } else {
                inFiles = new File[]{inFileOrFolder};
            }
            for (File inFile : inFiles) {
                downloadTasks.add(Executors.callable((new DownloadFurniSprites(inFile))));
            }
        }
    }

    private static void downloadMasks(List<Callable<Object>> downloadTasks) {
        if(!isLocal) {
            downloadTasks.add(Executors.callable(new DownloadMiscSprites("masks", "HabboRoomContent", true, "masks")));
        }
        else {
            File inFileOrFolder = new File("input\\masks\\");
            File[] inFiles;
            boolean singleFile = true;
            if (inFileOrFolder.isDirectory()) {
                singleFile = false;
                inFiles = inFileOrFolder.listFiles(OSCheck.getSwfFilter());
            } else {
                inFiles = new File[]{inFileOrFolder};
            }
            for (File inFile : inFiles) {
                downloadTasks.add(Executors.callable((new DownloadMiscSprites(inFile, true, "masks"))));
            }
        }
    }

    private static void downloadEffects(List<Callable<Object>> downloadTasks) throws MalformedURLException {
        if(!isLocal) {
            Effectmap effectmap = EffectmapParser.getEffectmap(
                    new URL(externalVariables.getFlashClientURL() + "effectmap.xml"));
            if (effectmap != null) {
                downloadTasks.addAll(effectmap.getEffects().stream()
                        .map(effect -> Executors.callable(new DownloadMiscSprites("sprites", effect.getLib(), true, "effects")))
                        .collect(Collectors.toList()));
            }
        }
        else {
            File inFileOrFolder = new File("input\\effects\\");
            File[] inFiles;
            boolean singleFile = true;
            if (inFileOrFolder.isDirectory()) {
                singleFile = false;
                inFiles = inFileOrFolder.listFiles(OSCheck.getSwfFilter());
            } else {
                inFiles = new File[]{inFileOrFolder};
            }
            for (File inFile : inFiles) {
                downloadTasks.add(Executors.callable((new DownloadMiscSprites(inFile, true, "effects"))));
            }
        }
    }

    private static void downloadFigures(List<Callable<Object>> downloadTasks) throws MalformedURLException {
        if(!isLocal) {
            URL p;
            try {
                p = new URL(externalVariables.getFigureMap());
            } catch (MalformedURLException e) {
                p = new URL(externalVariables.getFlashClientURL() + "figuremap.xml");
            }
            Figuremap figuremap = FiguremapParser.getFiguremap(p);

            if (figuremap != null) {
                downloadTasks.addAll(figuremap.getLib().stream()
                        .map(lib -> Executors.callable(new DownloadMiscSprites("sprites", lib.getId(), true, "clothes")))
                        .collect(Collectors.toList()));
            }
        }
        else {
            File inFileOrFolder = new File("input\\clothes\\");
            File[] inFiles;
            boolean singleFile = true;
            if (inFileOrFolder.isDirectory()) {
                singleFile = false;
                inFiles = inFileOrFolder.listFiles(OSCheck.getSwfFilter());
            } else {
                inFiles = new File[]{inFileOrFolder};
            }
            for (File inFile : inFiles) {
                downloadTasks.add(Executors.callable((new DownloadMiscSprites(inFile, true, "clothes"))));
            }
        }
    }

    private static void downloadPets(List<Callable<Object>> downloadTasks) {
        if(!isLocal) {
            downloadTasks.addAll(externalVariables.getPets().stream()
                    .map(pet -> Executors.callable(new DownloadMiscSprites("sprites", pet, false, "pets")))
                    .collect(Collectors.toList()));
        }
        else {
            File inFileOrFolder = new File("input\\pets\\");
            File[] inFiles;
            boolean singleFile = true;
            if (inFileOrFolder.isDirectory()) {
                singleFile = false;
                inFiles = inFileOrFolder.listFiles(OSCheck.getSwfFilter());
            } else {
                inFiles = new File[]{inFileOrFolder};
            }
            for (File inFile : inFiles) {
                downloadTasks.add(Executors.callable((new DownloadMiscSprites(inFile, false, "pets"))));
            }
        }
    }

    private static void downloadMisc(List<Callable<Object>> downloadTasks) {
        if(!isLocal) {
            downloadTasks.add(Executors.callable(new DownloadMiscSprites("sprites", "hh_human_body", true, "misc")));
            downloadTasks.add(Executors.callable(new DownloadMiscSprites("sprites", "hh_human_fx", true, "misc")));
            downloadTasks.add(Executors.callable(new DownloadMiscSprites("sprites", "hh_human_item", true, "misc")));
        }
        else {
            File inFileOrFolder = new File("input\\misc\\");
            File[] inFiles;
            boolean singleFile = true;
            if (inFileOrFolder.isDirectory()) {
                singleFile = false;
                inFiles = inFileOrFolder.listFiles(OSCheck.getSwfFilter());
            } else {
                inFiles = new File[]{inFileOrFolder};
            }
            for (File inFile : inFiles) {
                downloadTasks.add(Executors.callable((new DownloadMiscSprites(inFile, false, "pets"))));
            }
        }
    }
}
