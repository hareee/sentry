/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sentry.provider.db.service.persistent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import java.util.Map;
import org.apache.sentry.core.common.exception.SentryUserException;

public class SentryStoreSchemaInfo {
  private static final String SQL_FILE_EXTENSION = ".sql";
  private static final String UPGRADE_FILE_PREFIX = "upgrade-";
  private static final String INIT_FILE_PREFIX = "sentry-";
  private static final String VERSION_UPGRADE_LIST = "upgrade.order";
  private final String dbType;
  private final Map<String, List<String>> sentrySchemaVersions;
  private final String sentryScriptDir;

  private static final String SENTRY_VERSION = "2.1.0";

  public SentryStoreSchemaInfo(String sentryScriptDir, String dbType)
      throws SentryUserException {
    this.sentryScriptDir = sentryScriptDir;
    this.dbType = dbType;

    // load upgrade order for the given dbType
    String upgradeListFile = getSentryStoreScriptDir() + File.separator
        + VERSION_UPGRADE_LIST + "." + dbType;

    try {
      sentrySchemaVersions = SentryUpgradeOrder.readUpgradeGraph(new FileReader(upgradeListFile));
    } catch (FileNotFoundException e) {
      throw new SentryUserException("File " + upgradeListFile + " not found ", e);
    } catch (IOException e) {
      throw new SentryUserException("Error reading " + upgradeListFile, e);
    }
  }

  public String getSentrySchemaVersion() {
    return SENTRY_VERSION;
  }

  public List<String> getUpgradeScripts(String fromSchemaVer)
      throws SentryUserException {
    // check if we are already at current schema level
    if (getSentryVersion().equals(fromSchemaVer)) {
      return Collections.emptyList();
    }

    List<String> upgradePathList =
      SentryUpgradeOrder.getUpgradePath(sentrySchemaVersions, fromSchemaVer, getSentrySchemaVersion());
    if (upgradePathList.isEmpty()) {
      throw new SentryUserException("Unknown version specified for upgrade "
        + fromSchemaVer + " Sentry schema may be too old or newer");
    }

    // Create a new list with the script file paths of the upgrade order path obtained before
    List<String> upgradeScriptList = new LinkedList<>();
    for (String upgradePath : upgradePathList) {
      upgradeScriptList.add(generateUpgradeFileName(upgradePath));
    }

    return upgradeScriptList;
  }

  /***
   * Get the name of the script to initialize the schema for given version
   *
   * @param toVersion
   *          Target version. If it's null, then the current server version is
   *          used
   * @return
   * @throws SentryUserException
   */
  public String generateInitFileName(String toVersion, String scriptDir)
      throws SentryUserException {
    String version = toVersion;
    if (version == null) {
      version = getSentryVersion();
    }
    String initScriptName = INIT_FILE_PREFIX + dbType + "-" + version
        + SQL_FILE_EXTENSION;
    // check if the file exists
    if (!(new File(scriptDir + File.separatorChar
        + initScriptName).exists())) {
      throw new SentryUserException(
          "Unknown version specified for initialization: " + version);
    }
    return initScriptName;
  }

  /**
   * Find the directory of sentry store scripts
   *
   * @return
   */
  public String getSentryStoreScriptDir() {
    return sentryScriptDir;
  }

  // format the upgrade script name eg upgrade-x-y-dbType.sql
  private String generateUpgradeFileName(String fileVersion) {
    return INIT_FILE_PREFIX + UPGRADE_FILE_PREFIX + dbType + "-"
        + fileVersion + SQL_FILE_EXTENSION;
  }

  // Current hive version, in majorVersion.minorVersion.changeVersion format
  // TODO: store the version using the build script
  public static String getSentryVersion() {
    return SENTRY_VERSION;
  }
}
