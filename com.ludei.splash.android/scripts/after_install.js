#!/usr/bin/env node

/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

// This script modifies the project root's config.xml
// This restores the content tag's src attribute to its original value.

var fs = require('fs');
var path = require('path');

module.exports = function (context) {
    if (context.opts.cordova.platforms.indexOf('android') <= -1)
        return;

    var config_xml_path = path.join(context.opts.projectRoot, 'platforms', 'android', 'app', 'src', 'main', 'res', 'xml', 'config.xml');

    var et = context.requireCordovaModule('elementtree');
    var data = fs.readFileSync(config_xml_path).toString();
    var etree = et.parse(data);
    var item = null;
    var root = etree.getroot();
    for (var i = 0; i < root.getchildren().length; i++) {
        item = root.getItem(i);
        if (item.get("name", null) === "SplashPlugin") {
            root.delItem(i);
            break;
        }
    }

    if (item !== null) {
        root.append(item);
        data = etree.write({'indent': 4});
        fs.writeFileSync(config_xml_path, data);
    }

    var splash_path = path.join(context.opts.projectRoot, 'platforms', 'android', 'app', 'src', 'main', 'java', 'com', 'ludei', 'splash', 'android', 'SplashPlugin.java');
    var splash_data = fs.readFileSync(splash_path, 'utf8');
    if (root.attrib["android-packageName"])
        splash_data = splash_data.replace('_BUNDLE_ID_', root.attrib["android-packageName"]);
    else
        splash_data = splash_data.replace('_BUNDLE_ID_', root.attrib.id);
    var splash_data = fs.writeFileSync(splash_path, splash_data, 'utf8');
}