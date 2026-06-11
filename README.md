![Logo](./jamlin_logo.svg)

# JaMLin
**Ja**va **M**arkup **L**anguage **In**ternalisation is command line tool for internalisation of markup files (HTML, XML), that **keeps markup clean as it was meant to be**.

It's like **_gettext_** but without need to make markup dirty with anything special functions or custom attributes. It uses JSON files to store your strings. Send these files to your translators with link to [**JaMLin Editor**](https://github.com/StretchShop/jamlin-editor-webcomponent) app.


## What is it good for
For projects that use static HTML or XML that need to be translated and keep untouched otherwise. Sometimes you need to keep HTML pure in project without additional attributes, special tags or other languages involved, but with some internalisation tool.


## Quick start
1. In project root, find **jamlin_config.json** file - copy it to your destination folder and edit CSS selectors to extract what you need (see Config section for more info).
2. Build the project (see [Building and testing](#building-and-testing)) or find **jamlin-jar-with-dependencies.jar** inside the **target** directory. Copy the JAR to your destination folder, next to `jamlin_config.json`. It can't run without it.
3. Run as any other Java program using terminal (`java -jar jamlin-jar-with-dependencies.jar`) as described in next sections using any of 6 defined modes.


## Building and testing

### Prerequisites

* **Java 17** or newer (JDK)
* **Apache Maven 3.6+**

### Build

From the project root:

```bash
mvn clean package
```

This compiles the project, runs all tests, and produces two JAR files in `target/`:

| File | Description |
|------|-------------|
| `jamlin.jar` | Application JAR (dependencies not included) |
| `jamlin-jar-with-dependencies.jar` | Fat JAR with all dependencies — use this to run JaMLin |

### Test

Run the test suite only:

```bash
mvn test
```

Run the same check as CI (tests + package):

```bash
mvn verify
```

Tests use sample data from the `testdata/` directory. A full `mvn clean package` runs tests automatically before packaging.

### Continuous integration

GitHub Actions workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs `mvn verify` on push and pull requests to `main`/`master` using JDK 17.

### Run the built JAR

Example using the included test data:

```bash
cd testdata
java -jar ../target/jamlin-jar-with-dependencies.jar --action extract --source jamlin_demo.html --language sk
```

Additional CLI options:

* `--workingdir` / `-w` — set working directory (defaults to current directory)
* `--config` / `-c` — path to config file (defaults to `jamlin_config.json` in working directory)
* `--dictionary` / `-d` — build project dictionary on extract (default: `true`)

Exit codes: `0` success, `1` error, `2` invalid arguments.

For architecture notes and development history, see [info-base.md](./info-base.md).


## How it works
It works in 2 modes Extract and Replace. They are used to:

1. **EXTRACT strings from original HTML or XML.** (eg. index.html)
	* using CSS selectors from config file JSON.
	* creating new JSON where it extracts strings defined by selectors.
2. **REPLACE strings with localised ones.**
	* uses created JSON with translations.
	* creating new HTML or XML or replacing original files (eg. index-en.html).


## How to use it
To run it, you need too things defined:
1. Config file (required)
2. Parameters (optional)


### Config file
Every time you run JaMLin, it's looking for config file, named **jamlin_config.json** which defines all data needed for extraction in this structure:
```json
{
	"sources": {
		"directories": [
			{
				"path": "templates/",
				"extensions": [ "html", "xml" ],
				"traverse": false,
				"selectors": {
					"texts": {
						"type": "text",
						"selector": "p, a"
					},
					"params": {
						"type": "attribute",
						"selector": "a[title], img[title]",
						"attrName": "title"
					}
				}
			}
		],
		"files": [
			{
				"path": "test-*.html",
				"selectors": {
					"texts": {
						"type": "text",
						"selector": "p, a"
					},
					"params": {
						"type": "attribute",
						"selector": "a[title], img[title]",
						"attrName": "title"
					}
				}
			}
		]
	},
	"target": {
		"save_history": true,
		"replace_pattern": "*-$lang.*"
	}
}
```
It defines sources and targets:
1. In sources, you define what texts you want to use for translation. You can traverse whole directories for file with defined extensions or use only selected files. Within these two tools you define tags, you want to use using [CSS selectors](https://developer.mozilla.org/en-US/docs/Learn/CSS/Introduction_to_CSS/Selectors). You can define your own names for unlimited number of selectors using 3 types - text, attribute and value.
2. In target you define replace pattern of file and if existing file can be replaced (TODO).

You will probably need separate config for every project.

After you set it up, you can define parameters - action, source, target and language.

JaMLin by default uses "extract" action to traverse defined items and trying to extract their translations. When run with "replace" action, it tries to get new translations back to its original files.

### Parameters

Show all options and examples:

```bash
java -jar jamlin-jar-with-dependencies.jar --help
```

JaMLin receives several CLI parameters and works in 6 modes. Examples of using Jamlin looks like this:

`java -jar jamlin-jar-with-dependencies.jar
	--action "extract"
	--source "jamlin_demo.html"
	--language "sk"`

`java -jar jamlin-jar-with-dependencies.jar
	--action "replace"
	--source "jamlin_demo-extract.json"
	--target "jamlin_demo.html"
	--language "en"`

Parameters legend:

* **action** (required) - supports 2 values:
	* "extract" (default),
	* "replace".
* **source** - source file to read, differs according to action:
	* for "extract" action it's HTML file,
	* for "replace" action it's JSON file generated by "extract" command.
* **target** - target file is for one action only:
	* for "replace" action it's HTML file whose name will be used to create file names for target HTML files.
* **language** (optional) - language (eg. "en", "en\_US", "zh\_CN_#Hans", ...) works two ways:
	* for "extract" action it sets, what language version was used for extract strings,
	* for "replace" action sets what language you want to output.
* **dictionary** (optional) - creates project dictionary, true by default:
	* for "extract" action it creates project-dictionary.json file that contains all phrases and their translations from project.

According to parameters, **every action can have 3 modes**:

* **extract**
	1. **lang specific mode** - extract one language only. <br/>
	`java -jar jamlin-jar-with-dependencies.jar --action "extract" --source "jamlin_demo.html" --language "sk"`
	2. **semiautomatic mode** - extract and try to get language from source, if not use "xx". <br/>
	`java -jar jamlin-jar-with-dependencies.jar --action "extract" --source "jamlin_demo.html"`
	3. **full automatic mode** - extract all using config (default). Extract action is default. <br/>
	`java -jar jamlin-jar-with-dependencies.jar`
* **replace**
	1. **lang specific mode** - replace one language only. <br/>
	`java -jar jamlin-jar-with-dependencies.jar --action "replace" --source "jamlin_demo-extract.json" --target "jamlin_demo.html" --language "en"`
	2. **semiautomatic mode** - using selected target replace all languages from translation source and create related output files. <br/>
	`java -jar jamlin-jar-with-dependencies.jar --action "replace" --source "jamlin_demo-extract.json" --target "jamlin_demo.html"`
	3. **full automatic mode** - replace all using config (default). <br/>
	`java -jar jamlin-jar-with-dependencies.jar --action "replace"`


## Translation
JaMLin does not translate data by itself. It only extracts strings according to your config. To translate it, you need to fill translations into JSON file using this format:
```json
{
  "translationBlocks": [
    {
      "name": "texts",
      "cssSelector": "p, a",
      "type": "text",
      "translationStrings": [
        {
          "stringOrig": "Menu",
          "selector": "#mobile_menu",
          "translations": [
            {
              "langCode": "en",
              "translation": "Menu EN version"
            },
            {
              "langCode": "sk",
              "translation": "Menu SK version"
            }
          ]
        }
      ]
    }
  ]
}
```
For that, you can use any editor, that can read JSON files or try [**JaMLin Editor**](https://github.com/Wradgio/Jamlin-Editor/). Check binaries downloads in [Jamlin Editor releases](https://github.com/Wradgio/Jamlin-Editor/releases) - https://github.com/Wradgio/Jamlin-Editor/releases.

## Command line shortcut
If you want to have Jamlin available everywhere without need to write whole "java -jar /whole/path/to/jamlin.jar", you can create shortcut into **~/.profile** (or ~/.bash_profile) in your home folder - just add this line in it:

`alias jamlin="java -jar /home/batman/apps/jamlin.jar"`

It should work on terminal windows opened after you saved that.

## Why is it such a mess
It's my first tool in Java and Maven, a one man project. It will take some time to clean it up. Any help is appreciated.
