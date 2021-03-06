/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.version;

import java.util.HashMap;
import java.util.Map;

/**
 * User: freds Date: May 29, 2008 Time: 10:15:59 AM
 */
public enum ArtifactoryVersion {
    v122rc0("1.2.2-rc0", 804),
    v122rc1("1.2.2-rc1", 819),
    v122rc2("1.2.2-rc2", 826),
    v122("1.2.2", 836),
    v125rc0("1.2.5-rc0", 970),
    v125rc1("1.2.5-rc1", 1015),
    v125rc2("1.2.5-rc2", 1082),
    v125rc3("1.2.5-rc3", 1087),
    v125rc4("1.2.5-rc4", 1104),
    v125rc5("1.2.5-rc5", 1115),
    v125rc6("1.2.5-rc6", 1136),
    v125("1.2.5", 1154),
    v125u1("1.2.5u1", 1174),
    v130beta1("1.3.0-beta-1", 1501),
    v130beta2("1.3.0-beta-2", 1509),
    v130beta3("1.3.0-beta-3", 1992),
    v130beta4("1.3.0-beta-4", 2065),
    v130beta5("1.3.0-beta-5", 2282),
    v130beta6("1.3.0-beta-6", 2862),
    v130beta61("1.3.0-beta-6.1", 2897),
    v130rc1("1.3.0-rc-1", 3148),
    v130rc2("1.3.0-rc-2", 3392),
    v200("2.0.0", 3498),
    v201("2.0.1", 3768),
    v202("2.0.2", 3947),
    v203("2.0.3", 4468),
    v204("2.0.4", 4781),
    v205("2.0.5", 4903),
    v206("2.0.6", 5625),
    v207("2.0.7", 7453),
    v208("2.0.8", 7829),
    v210("2.1.0", 8350),
    v211("2.1.1", 8514),
    v212("2.1.2", 8706),
    v213("2.1.3", 9204),
    v220("2.2.0", 9932),
    v221("2.2.1", 10024),
    v222("2.2.2", 10427),
    v223("2.2.3", 10588),
    v224("2.2.4", 11256),
    v225("2.2.5", 11524),
    v230("2.3.0", 12450),
    v231("2.3.1", 12714),
    v232("2.3.2", 13005),
    v233("2.3.3", 13011),
    v2331("2.3.3.1", 13012),
    v234("2.3.4", 13017),
    v2341("2.3.4.1", 13021),
    v240("2.4.0", 13048),
    v241("2.4.1", 13050),
    v242("2.4.2", 13059),
    v250("2.5.0", 13086),
    v251("2.5.1", 13089),
    v2511("2.5.1.1", 13098),
    v252("2.5.2", 13110),
    v260("2.6.0", 13119),
    v261("2.6.1", 13124),
    v262("2.6.2", 13147),
    v263("2.6.3", 13148),
    v264("2.6.4", 13153),
    v265("2.6.5", 13174),
    v266("2.6.6", 13183),
    v267("2.6.7", 13201),
    v300("3.0.0", Integer.MAX_VALUE);

    public static ArtifactoryVersion getCurrent() {
        ArtifactoryVersion[] versions = ArtifactoryVersion.values();
        return versions[versions.length - 1];
    }

    private final String value;
    private final int revision;
    private final Map<String, SubConfigElementVersion> subConfigElementVersionsByClass =
            new HashMap<String, SubConfigElementVersion>();

    ArtifactoryVersion(String value, int revision) {
        this.value = value;
        this.revision = revision;
    }

    public static <T extends SubConfigElementVersion> void addSubConfigElementVersion(T scev,
            VersionComparator versionComparator) {
        ArtifactoryVersion[] versions = values();
        for (ArtifactoryVersion version : versions) {
            if (versionComparator.supports(version)) {
                version.subConfigElementVersionsByClass.put(scev.getClass().getName(), scev);
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    public <T extends SubConfigElementVersion> T getSubConfigElementVersion(Class<T> subConfigElementVersion) {
        return (T) subConfigElementVersionsByClass.get(subConfigElementVersion.getName());
    }

    public String getValue() {
        return value;
    }

    public long getRevision() {
        return revision;
    }

    public boolean isCurrent() {
        return this == getCurrent();
    }

    /**
     * @param otherVersion The other ArtifactoryVersion
     * @return returns true if this version is before the other version
     */
    public boolean before(ArtifactoryVersion otherVersion) {
        return this.compareTo(otherVersion) < 0;
    }

    /**
     * @param otherVersion The other ArtifactoryVersion
     * @return returns true if this version is after the other version
     */
    public boolean after(ArtifactoryVersion otherVersion) {
        return this.compareTo(otherVersion) > 0;
    }

    /**
     * @param otherVersion The other ArtifactoryVersion
     * @return returns true if this version is before or equal to the other version
     */
    public boolean beforeOrEqual(ArtifactoryVersion otherVersion) {
        return this == otherVersion || this.compareTo(otherVersion) < 0;
    }
}