package io.canvasmc.horizon.util;

import org.jspecify.annotations.NonNull;

/**
 * The Minecraft version API is used to document which versions Horizon supports, and for supporting the Minecraft
 * dependency for Horizon plugins, and offering those same plugins with a versatile and informative API for different
 * supported Minecraft versions
 *
 * @author dueris
 * @apiNote Horizon includes snapshots, release candidates, and pre-releases, so that Horizon is capable of doing
 *     development snapshot versions if Paper chooses to release those on GitHub or to their downloads API
 */
public enum MinecraftVersion {
    /**
     * Unknown name, future drop
     */
    V26_1_SNAPSHOT_7("26.1-snapshot-7", "26.1 Snapshot 7", 25, VersionType.SNAPSHOT),
    V26_1_SNAPSHOT_6("26.1-snapshot-6", "26.1 Snapshot 6", 25, VersionType.SNAPSHOT),
    V26_1_SNAPSHOT_5("26.1-snapshot-5", "26.1 Snapshot 5", 25, VersionType.SNAPSHOT),
    V26_1_SNAPSHOT_4("26.1-snapshot-4", "26.1 Snapshot 4", 25, VersionType.SNAPSHOT),
    V26_1_SNAPSHOT_3("26.1-snapshot-3", "26.1 Snapshot 3", 25, VersionType.SNAPSHOT),
    V26_1_SNAPSHOT_2("26.1-snapshot-2", "26.1 Snapshot 2", 25, VersionType.SNAPSHOT),
    V26_1_SNAPSHOT_1("26.1-snapshot-1", "26.1 Snapshot 1", 25, VersionType.SNAPSHOT),

    /**
     * <a href="https://minecraft.wiki/w/Mounts_of_Mayhem">Mounts of Mayhem</a> drop
     */
    V1_21_11("1.21.11", "1.21.11", 21, VersionType.STABLE),
    V1_21_11_RC3("1.21.11-rc3", "1.21.11 Release Candidate 3", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_11_RC2("1.21.11-rc2", "1.21.11 Release Candidate 2", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_11_RC1("1.21.11-rc1", "1.21.11 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_11_PR5("1.21.11-pre5", "1.21.11 Pre-Release 5", 21, VersionType.PRE_RELEASE),
    V1_21_11_PR4("1.21.11-pre4", "1.21.11 Pre-Release 4", 21, VersionType.PRE_RELEASE),
    V1_21_11_PR3("1.21.11-pre3", "1.21.11 Pre-Release 3", 21, VersionType.PRE_RELEASE),
    V1_21_11_PR2("1.21.11-pre2", "1.21.11 Pre-Release 2", 21, VersionType.PRE_RELEASE),
    V1_21_11_PR1("1.21.11-pre1", "1.21.11 Pre-Release 1", 21, VersionType.PRE_RELEASE),
    V_25W46A("25w46a", "25w46a", 21, VersionType.SNAPSHOT),
    V_25W45A("25w45a", "25w45a", 21, VersionType.SNAPSHOT),
    V_25W44A("25w44a", "25w44a", 21, VersionType.SNAPSHOT),
    V_25W43A("25w43a", "25w43a", 21, VersionType.SNAPSHOT),
    V_25W42A("25w42a", "25w42a", 21, VersionType.SNAPSHOT),
    V_25W41A("25w41a", "25w41a", 21, VersionType.SNAPSHOT),

    /**
     * Hotfix update for {@link io.canvasmc.horizon.util.MinecraftVersion#V1_21_9}
     */
    V1_21_10("1.21.10", "1.21.10", 21, VersionType.STABLE),
    V1_21_10_RC1("1.21.10-rc1", "1.21.10 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE),

    /**
     * <a href="https://minecraft.wiki/w/The_Copper_Age">The Copper Age</a> drop
     */
    V1_21_9("1.21.9", "1.21.9", 21, VersionType.STABLE),
    V1_21_9_RC1("1.21.9-rc1", "1.21.9 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_9_PR4("1.21.9-pre4", "1.21.9 Pre-Release 4", 21, VersionType.PRE_RELEASE),
    V1_21_9_PR3("1.21.9-pre3", "1.21.9 Pre-Release 3", 21, VersionType.PRE_RELEASE),
    V1_21_9_PR2("1.21.9-pre2", "1.21.9 Pre-Release 2", 21, VersionType.PRE_RELEASE),
    V1_21_9_PR1("1.21.9-pre1", "1.21.9 Pre-Release 1", 21, VersionType.PRE_RELEASE),
    V_25W37A("25w37a", "25w37a", 21, VersionType.SNAPSHOT),
    V_25W36B("25w36b", "25w36b", 21, VersionType.SNAPSHOT),
    V_25W36A("25w36a", "25w36a", 21, VersionType.SNAPSHOT),
    V_25W35A("25w35a", "25w35a", 21, VersionType.SNAPSHOT),
    V_25W34B("25w34b", "25w34b", 21, VersionType.SNAPSHOT),
    V_25W34A("25w34a", "25w34a", 21, VersionType.SNAPSHOT),
    V_25W33A("25w33a", "25w33a", 21, VersionType.SNAPSHOT),
    V_25W32A("25w32a", "25w32a", 21, VersionType.SNAPSHOT),
    V_25W31A("25w31a", "25w31a", 21, VersionType.SNAPSHOT),

    /**
     * Second hotfix update for {@link io.canvasmc.horizon.util.MinecraftVersion#V1_21_6}
     */
    V1_21_8("1.21.8", "1.21.8", 21, VersionType.STABLE),
    V1_21_8_RC1("1.21.8-rc1", "1.21.8 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE),

    /**
     * First hotfix update for {@link io.canvasmc.horizon.util.MinecraftVersion#V1_21_6}
     */
    V1_21_7("1.21.7", "1.21.7", 21, VersionType.STABLE),
    V1_21_7_RC2("1.21.7-rc2", "1.21.7 Release Candidate 2", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_7_RC1("1.21.7-rc1", "1.21.7 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE),

    /**
     * <a href="https://minecraft.wiki/w/Chase_the_Skies">Chase the Skies</a> drop
     */
    V1_21_6("1.21.6", "1.21.6", 21, VersionType.STABLE),
    V1_21_6_RC1("1.21.6-rc1", "1.21.6 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_6_PR4("1.21.6-pre4", "1.21.6 Pre-Release 4", 21, VersionType.PRE_RELEASE),
    V1_21_6_PR3("1.21.6-pre3", "1.21.6 Pre-Release 3", 21, VersionType.PRE_RELEASE),
    V1_21_6_PR2("1.21.6-pre2", "1.21.6 Pre-Release 2", 21, VersionType.PRE_RELEASE),
    V1_21_6_PR1("1.21.6-pre1", "1.21.6 Pre-Release 1", 21, VersionType.PRE_RELEASE),
    V_25W21A("25w21a", "25w21a", 21, VersionType.SNAPSHOT),
    V_25W20A("25w20a", "25w20a", 21, VersionType.SNAPSHOT),
    V_25W19A("25w19a", "25w19a", 21, VersionType.SNAPSHOT),
    V_25W18A("25w18a", "25w18a", 21, VersionType.SNAPSHOT),
    V_25W17A("25w17a", "25w17a", 21, VersionType.SNAPSHOT),
    V_25W16A("25w16a", "25w16a", 21, VersionType.SNAPSHOT),
    V_25W15A("25w15a", "25w15a", 21, VersionType.SNAPSHOT),

    /**
     * <a href="https://minecraft.wiki/w/Spring_to_Life">Spring to Life</a> drop
     */
    V1_21_5("1.21.5", "1.21.5", 21, VersionType.STABLE),
    V1_21_5_RC2("1.21.5-rc2", "1.21.5 Release Candidate 2", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_5_RC1("1.21.5-rc1", "1.21.5 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_5_PR3("1.21.5-pre3", "1.21.5 Pre-Release 3", 21, VersionType.PRE_RELEASE),
    V1_21_5_PR2("1.21.5-pre2", "1.21.5 Pre-Release 2", 21, VersionType.PRE_RELEASE),
    V1_21_5_PR1("1.21.5-pre1", "1.21.5 Pre-Release 1", 21, VersionType.PRE_RELEASE),
    V_25W10A("25w10a", "25w10a", 21, VersionType.SNAPSHOT),
    V_25W09B("25w09b", "25w09b", 21, VersionType.SNAPSHOT),
    V_25W09A("25w09a", "25w09a", 21, VersionType.SNAPSHOT),
    V_25W08A("25w08a", "25w08a", 21, VersionType.SNAPSHOT),
    V_25W07A("25w07a", "25w07a", 21, VersionType.SNAPSHOT),
    V_25W06A("25w06a", "25w06a", 21, VersionType.SNAPSHOT),
    V_25W05A("25w05a", "25w05a", 21, VersionType.SNAPSHOT),
    V_25W04A("25w04a", "25w04a", 21, VersionType.SNAPSHOT),
    V_25W03A("25w03a", "25w03a", 21, VersionType.SNAPSHOT),
    V_25W02A("25w02a", "25w02a", 21, VersionType.SNAPSHOT),

    /**
     * <a href="https://minecraft.wiki/w/The_Garden_Awakens">The Garden Awakens</a> drop
     */
    V1_21_4("1.21.4", "1.21.4", 21, VersionType.STABLE),
    V1_21_4_RC3("1.21.4-rc3", "1.21.4 Release Candidate 3", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_4_RC2("1.21.4-rc2", "1.21.4 Release Candidate 2", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_4_RC1("1.21.4-rc1", "1.21.4 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_4_PR3("1.21.4-pre3", "1.21.4 Pre-Release 3", 21, VersionType.PRE_RELEASE),
    V1_21_4_PR2("1.21.4-pre2", "1.21.4 Pre-Release 2", 21, VersionType.PRE_RELEASE),
    V1_21_4_PR1("1.21.4-pre1", "1.21.4 Pre-Release 1", 21, VersionType.PRE_RELEASE),
    V_24W46A("24w46a", "24w46a", 21, VersionType.SNAPSHOT),
    V_24W45A("24w45a", "24w45a", 21, VersionType.SNAPSHOT),
    V_24W44A("24w44a", "24w44a", 21, VersionType.SNAPSHOT),

    /**
     * Hotfix update for {@link io.canvasmc.horizon.util.MinecraftVersion#V1_21_2}
     */
    V1_21_3("1.21.3", "1.21.3", 21, VersionType.STABLE),

    /**
     * <a href="https://minecraft.wiki/w/Bundles_of_Bravery">Bundles of Bravery</a> drop
     */
    V1_21_2("1.21.2", "1.21.2", 21, VersionType.STABLE),
    V1_21_2_RC2("1.21.2-rc2", "1.21.2 Release Candidate 2", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_2_RC1("1.21.2-rc1", "1.21.2 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_2_PR5("1.21.2-pre5", "1.21.2 Pre-Release 5", 21, VersionType.PRE_RELEASE),
    V1_21_2_PR4("1.21.2-pre4", "1.21.2 Pre-Release 4", 21, VersionType.PRE_RELEASE),
    V1_21_2_PR3("1.21.2-pre3", "1.21.2 Pre-Release 3", 21, VersionType.PRE_RELEASE),
    V1_21_2_PR2("1.21.2-pre2", "1.21.2 Pre-Release 2", 21, VersionType.PRE_RELEASE),
    V1_21_2_PR1("1.21.2-pre1", "1.21.2 Pre-Release 1", 21, VersionType.PRE_RELEASE),
    V_24W40A("24w40a", "24w40a", 21, VersionType.SNAPSHOT),
    V_24W39A("24w39a", "24w39a", 21, VersionType.SNAPSHOT),
    V_24W38A("24w38a", "24w38a", 21, VersionType.SNAPSHOT),
    V_24W37A("24w37a", "24w37a", 21, VersionType.SNAPSHOT),
    V_24W36A("24w36a", "24w36a", 21, VersionType.SNAPSHOT),
    V_24W35A("24w35a", "24w35a", 21, VersionType.SNAPSHOT),
    V_24W34A("24w34a", "24w34a", 21, VersionType.SNAPSHOT),
    V_24W33A("24w33a", "24w33a", 21, VersionType.SNAPSHOT),

    /**
     * Hotfix update for {@link io.canvasmc.horizon.util.MinecraftVersion#V1_21}
     */
    V1_21_1("1.21.1", "1.21.1", 21, VersionType.STABLE),
    V1_21_1_RC1("1.21.1-rc1", "1.21.1 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE),

    /**
     * <a href="https://minecraft.wiki/w/Tricky_Trials">Tricky Trials</a> drop
     */
    V1_21("1.21", "1.21", 21, VersionType.STABLE),
    V1_21_RC1("1.21-rc1", "1.21 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE),
    V1_21_PR4("1.21-pre4", "1.21 Pre-Release 4", 21, VersionType.PRE_RELEASE),
    V1_21_PR3("1.21-pre3", "1.21 Pre-Release 3", 21, VersionType.PRE_RELEASE),
    V1_21_PR2("1.21-pre2", "1.21 Pre-Release 2", 21, VersionType.PRE_RELEASE),
    V1_21_PR1("1.21-pre1", "1.21 Pre-Release 1", 21, VersionType.PRE_RELEASE),
    V_24W21B("24w21b", "24w21b", 21, VersionType.SNAPSHOT),
    V_24W21A("24w21a", "24w21a", 21, VersionType.SNAPSHOT),
    V_24W20A("24w20a", "24w20a", 21, VersionType.SNAPSHOT),
    V_24W19B("24w19b", "24w19b", 21, VersionType.SNAPSHOT),
    V_24W19A("24w19a", "24w19a", 21, VersionType.SNAPSHOT),
    V_24W18A("24w18a", "24w18a", 21, VersionType.SNAPSHOT),

    /**
     * Hotfix update for the <a href="https://minecraft.wiki/w/Armored_Paws">Armored Paws</a> drop, lowest supported
     * version of Minecraft for Horizon
     */
    V1_20_6("1.20.6", "1.20.6", 21, VersionType.STABLE),
    V1_20_6_RC1("1.20.6-rc1", "1.20.6 Release Candidate 1", 21, VersionType.RELEASE_CANDIDATE);

    private final String id;
    private final String name;
    private final int javaVersion;
    private final VersionType type;

    MinecraftVersion(String id, String name, int javaVersion, VersionType type) {
        this.id = id;
        this.name = name;
        this.javaVersion = javaVersion;
        this.type = type;
    }

    /**
     * Gets the Minecraft version from the version id, like {@code 26.1-snapshot-4} would return
     * {@link io.canvasmc.horizon.util.MinecraftVersion#V26_1_SNAPSHOT_4}
     *
     * @param id
     *     the version id, which is case-sensitive
     *
     * @return the Minecraft version associated with the id provided
     */
    public static @NonNull MinecraftVersion fromStringId(String id) {
        for (final MinecraftVersion value : MinecraftVersion.values()) {
            if (id.equals(value.id)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown Minecraft version '" + id + "'");
    }

    /**
     * Compares this Minecraft version to another, returning if this version is newer
     *
     * @param other
     *     the Minecraft version to compare to
     *
     * @return {@code true} if this version is newer than the comparison, {@code false} otherwise
     *
     * @apiNote Versions are ordered from newest {@code (ordinal 0)} to oldest {@code (highest ordinal)} in the
     *     enum
     */
    public boolean isNewerThan(@NonNull MinecraftVersion other) {
        return this.ordinal() < other.ordinal();
    }

    /**
     * Compares this Minecraft version to another, returning if this version is older
     *
     * @param other
     *     the Minecraft version to compare to
     *
     * @return {@code true} if this version is older than the comparison, {@code false} otherwise
     *
     * @apiNote Versions are ordered from newest {@code (ordinal 0)} to oldest {@code (highest ordinal)} in the
     *     enum
     */
    public boolean isOlderThan(@NonNull MinecraftVersion other) {
        return this.ordinal() > other.ordinal();
    }

    /**
     * Compares this Minecraft version to another, returning if this version is newer or the same
     *
     * @param other
     *     the Minecraft version to compare to
     *
     * @return {@code true} if this version is newer or the same as the comparison, {@code false} otherwise
     *
     * @apiNote Versions are ordered from newest {@code (ordinal 0)} to oldest {@code (highest ordinal)} in the
     *     enum
     */
    public boolean isNewerThanOrEqualTo(@NonNull MinecraftVersion other) {
        return this.ordinal() <= other.ordinal();
    }

    /**
     * Compares this Minecraft version to another, returning if this version is older or the same
     *
     * @param other
     *     the Minecraft version to compare to
     *
     * @return {@code true} if this version is older or the same as the comparison, {@code false} otherwise
     *
     * @apiNote Versions are ordered from newest {@code (ordinal 0)} to oldest {@code (highest ordinal)} in the
     *     enum
     */
    public boolean isOlderThanOrEqualTo(@NonNull MinecraftVersion other) {
        return this.ordinal() >= other.ordinal();
    }

    /**
     * Gets the version id associated with this version. For some versions, this matches the actual name, like snapshots
     * and full releases
     *
     * @return the version id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the readable name associated with this version, like {@code 26.1 Snapshot 2}
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the required Java version for this version of Minecraft
     *
     * @return the Java version requirement
     */
    public int getJavaVersion() {
        return javaVersion;
    }

    /**
     * Gets the version type this is, like {@code SNAPSHOT}, {@code STABLE}, {@code PRE_RELEASE}, etc
     *
     * @return the version type
     */
    public VersionType getType() {
        return type;
    }

    /**
     * The Minecraft version type, primarily useful for differentiating between types technically, and apply filtering
     */
    public enum VersionType {
        SNAPSHOT,
        RELEASE_CANDIDATE,
        PRE_RELEASE,
        STABLE
    }
}
