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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Azure Camera Server - Furni Sprite Extractor
 * Written by Scott Stamp (scottstamp851, scott@hypermine.com)
 */
public class SpriteExtractor {
    public static ExternalVariables externalVariables;
    private static final boolean downloadMisc = false;
    private static final boolean downloadMasks = false;
    private static boolean downloadPets =false;
    private static boolean downloadFigures = false;
    private static boolean downloadEffects =false;
    private static boolean downloadFurni = true;
    public static boolean isLocal = false;




    public static void main(String[] args) throws Throwable {
        Log.println("               Azure Camera Server - Sprite Extractor");
        Log.println("      Written by Scott Stamp (scottstamp851, scott@hypermine.com)");
        Log.println();
        Log.println("Note: Some files may fail to download/extract. Common with low revision ID numbers.");
        Log.println("      It means they're no longer hosted on Habbo's server. This is normal.");
        Log.println();
        ExecutorService downloadPool = Executors.newFixedThreadPool(8);
        List<Callable<Object>> downloadTasks = new ArrayList<>();

        // Let's get command line arguments
        if(args.length > 0) {
            try {
                externalVariables = new ExternalVariables(new URL(args[0]));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            downloadPets = args[1].equalsIgnoreCase("true");
            downloadFigures = args[2].equalsIgnoreCase("true");
            downloadEffects = args[3].equalsIgnoreCase("true");
            downloadFurni = args[4].equalsIgnoreCase("true");
        }
        else
        {
            try {
                //externalVariables = new ExternalVariables(new URL("https://www.habbo.com/gamedata/external_variables/1"));
                isLocal = true;
                downloadPets = false;
                downloadFigures = true;
                downloadEffects = false;
                downloadFurni = true;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        // Download misc. files (specified in config_habbo.xml)
        if (downloadMisc) downloadMisc(downloadTasks);
        // Download all pet sprites
        if (downloadPets) downloadPets(downloadTasks);
        // Download all figure sprites
        if (downloadFigures) downloadFigures(downloadTasks);
        // Download all effects sprites
        if (downloadEffects) downloadEffects(downloadTasks);
        // Download all wall/floor/landscape masks
        if (downloadMasks) downloadMasks(downloadTasks);
        // Download all furni sprites
        if (downloadFurni) downloadFurni(downloadTasks);

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
            File inFileOrFolder = new File("swf\\furnis\\");
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
        downloadTasks.add(Executors.callable(new DownloadMiscSprites("masks", "HabboRoomContent", true, "mpu")));
    }

    private static void downloadEffects(List<Callable<Object>> downloadTasks) throws MalformedURLException {
        Effectmap effectmap = EffectmapParser.getEffectmap(
                new URL(externalVariables.getFlashClientURL() + "effectmap.xml"));

        if (effectmap != null) {
            downloadTasks.addAll(effectmap.getEffects().stream()
                    .map(effect -> Executors.callable(new DownloadMiscSprites("sprites", effect.getLib(), true, "effects")))
                    .collect(Collectors.toList()));
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
            File inFileOrFolder = new File("swf\\gordon\\");
            File[] inFiles;
            boolean singleFile = true;
            if (inFileOrFolder.isDirectory()) {
                singleFile = false;
                inFiles = inFileOrFolder.listFiles(OSCheck.getSwfFilter());
            } else {
                inFiles = new File[]{inFileOrFolder};
            }
            for (File inFile : inFiles) {
                downloadTasks.add(Executors.callable((new DownloadMiscSprites(inFile, true))));
            }
        }
    }

    private static void downloadPets(List<Callable<Object>> downloadTasks) {
        downloadTasks.addAll(externalVariables.getPets().stream()
                .map(pet -> Executors.callable(new DownloadMiscSprites("sprites", pet, false, "pets")))
                .collect(Collectors.toList()));
    }

    private static void downloadMisc(List<Callable<Object>> downloadTasks) {
        downloadTasks.add(Executors.callable(new DownloadMiscSprites("sprites", "hh_human_body", true, "misc")));
        downloadTasks.add(Executors.callable(new DownloadMiscSprites("sprites", "hh_human_fx", true, "misc")));
        downloadTasks.add(Executors.callable(new DownloadMiscSprites("sprites", "hh_human_item", true, "misc")));
    }
}
