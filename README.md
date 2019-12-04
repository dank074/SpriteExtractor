# SpriteExtractor
Small Java application to download and extract all furni sprites.
Can extract furni, pet, clothes, masks, and effects sprites. Will also extract the manifest files for non-furni files.

# Usage
`java -jar SpriteExtractor.jar <options>`

Available options:
```
-r, --remote <url>    external variables url
    --save            save remote downloaded swfs
-l, --local           use local directory files

-c, --clothes         extract clothes sprites
-e, --effects         extract effects sprites
-f, --furnis          extract furni sprites
-p, --pets            extract pet sprites
-m, --miscellaneous   extract misc sprites defined in config_habbo
-k, --masks           extract wall/floor/background sprites
```

## Examples

### Remote swf files
for using remote swf files, extracting effects and clothes and saving swf files locally:
 ```
 java -jar SpriteExtractor.jar -r https://www.habbo.com/gamedata/external_variables/1 --save -c -e
 ```
 
### Local swf files 
for using local swf files and extracting clothes:
```
java -jar SpriteExtractor.jar -l -c
```

For local swf files the directories must be set in the following format
```
.
├── ...
├── input                   
│   ├── clothes
│   ├── pets
│   ├── effects 
│   ├── furnis
│   ├── misc
│   ├── masks 
└── SpriteExtractor.jar
```

 
 
