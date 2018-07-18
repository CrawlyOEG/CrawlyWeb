WebCrawler
===================================================

`CrawlyWeb` is a website that facilitates [Crawly Project](https://github.com/CrawlyOEG) operations.

© 2018 Jorge Galán - OEG-UPM. Available under Apache License 2.0. See [`LICENSE`](LICENSE).

## Features

- Know the belonging percentage of an article to alight pollution topic in the *discover section*
- Extract the resources from a pdf in the *extract section*
- Create your own model in the *make your own section*

## Requirements

- If you want to build the code from source, you need all the libraries.

## Download

Download one version of all the modules from our [releases page](../../releases), that includes a jar.

## Usage
From jar run:
```
$java -jar Crawly-1.0.jar folderTopics folderPDF folderLibrarly
```
Where files uploaded in *uploadFile* will be temporarily saved in folderTopics, *checkPDF* files uploaded will be temporarily saved in folderPDF and *makeModel* files 
will be temporarily saved in folderLibrarly 

Use the page at localhost:8080

## Building from Source

Clone this repo and run:
```
mvn clean compile assembly:single
```

Then, get your own version of the jar in the project's `target` folder.

<a title="OEG Laboratory" href="http://www.oeg-upm.net/" target="_blank"><img alt="OEG Laboratory" src="http://stars4all.eu/wp-content/uploads/2016/10/OEG.png" width="220" height="220"></a>
<a title="STARS4ALL" href="http://stars4all.eu" target="_blank"><img alt="STARS4ALL" src="http://linkeddata4.dia.fi.upm.es/wordpress-new/wp-content/uploads/2016/12/logo_dark.png" width="220" height="220"></a>
