/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.resources;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.resources.ResourceTypes.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.HashMap;

@ApiStatus.Internal
public class AssetManager {

    public static final int kInvalidCookie = -1;
    public static final int kMaxIterations = 20;

    public static class ResolvedBag {
        public static final int COLUMN_TYPE = 0;
        public static final int COLUMN_DATA = 1;
        public static final int COLUMN_COOKIE = 2;
        public static final int VALUE_COLUMNS = 3;

        public int typeSpecFlags;

        /*
         * This holds (namespace, attribute name) pairs, sorted by namespace and then
         * by attribute name in lexicographic order.
         */
        public String[] keys;
        /*
            struct Value {
                uint32_t type;
                uint32_t data;
                int32_t cookie;
            }
         */
        public int[] values;

        public String namespace(int index) {
            return keys[index << 1];
        }

        public String attribute(int index) {
            return keys[(index << 1) + 1];
        }

        public int type(int index) {
            return values[index * VALUE_COLUMNS + COLUMN_TYPE];
        }

        public int data(int index) {
            return values[index * VALUE_COLUMNS + COLUMN_DATA];
        }

        public int cookie(int index) {
            return values[index * VALUE_COLUMNS + COLUMN_COOKIE];
        }

        public int getEntryCount() {
            assert values == null ||
                    (values.length / VALUE_COLUMNS) == (keys.length >> 1);
            return keys != null ? keys.length >> 1 : 0;
        }
    }

    public static class PackageGroup {
        // The following three arrays are parallel arrays
        public final ArrayList<LoadedPackage> packages = new ArrayList<>();

        // Array of maps from type index to prefiltered collection of configurations
        // that match the current AssetManager configuration
        public final ArrayList<ArrayList<LoadedPackage.TypeSpec.TypeEntry>[]> filteredConfigs = new ArrayList<>();

        public final IntArrayList cookies = new IntArrayList();
    }

    public static class FindEntryResult {

        public int cookie;
        // The value of the resource table entry.
        // Pointer to a ResTable_entry struct.
        public long entry;
        public int typeFlags;

        public FindEntryResult(int cookie, long entry, int typeFlags) {
            this.cookie = cookie;
            this.entry = entry;
            this.typeFlags = typeFlags;
        }

        public void set(FindEntryResult o) {
            cookie = o.cookie;
            entry = o.entry;
            typeFlags = o.typeFlags;
        }
    }

    @Nullable
    private FindEntryResult findEntry(String namespace, String typeString, String entryString) {
        if (namespace == null || namespace.isEmpty() ||
                typeString == null || typeString.isEmpty() ||
                entryString == null || entryString.isEmpty()) {
            return null;
        }

        PackageGroup packageGroup = packageGroups.get(namespace);
        if (packageGroup == null) {
            return null;
        }

        return findEntryInternal(packageGroup, typeString, entryString);
    }

    @Nullable
    private FindEntryResult findEntryInternal(@NonNull PackageGroup packageGroup,
                                              @NonNull String typeString, @NonNull String entryString) {
        int bestCookie = kInvalidCookie;
        LoadedPackage bestPackage = null;
        long bestType = 0;
        int bestOffset = 0;

        int packageCount = packageGroup.packages.size();
        for (int pi = 0; pi < packageCount; pi++) {
            LoadedPackage loadedPackage = packageGroup.packages.get(pi);
            int cookie = packageGroup.cookies.getInt(pi);

            int typeIndex = loadedPackage.getTypeStringTable().indexOfString(typeString);
            int entryIndex = loadedPackage.getKeyStringTable().indexOfString(entryString);

            LoadedPackage.TypeSpec typeSpec = loadedPackage.getTypeSpecByTypeIndex(typeIndex);
            if (typeSpec == null) {
                continue;
            }

            int typeEntryCount = typeSpec.typeEntries.length;
            for (int i = 0; i < typeEntryCount; i++) {
                LoadedPackage.TypeSpec.TypeEntry typeEntry = typeSpec.typeEntries[i];

                long type = typeEntry.type;
                int offset = loadedPackage.getEntryOffset(type, entryIndex);

                if (offset == ResTable_type.NO_ENTRY) {
                    continue;
                }

                bestCookie = cookie;
                bestPackage = loadedPackage;
                bestType = type;
                bestOffset = offset;

                break;
            }
        }

        if (bestCookie == kInvalidCookie) {
            return null;
        }

        long bestEntry = bestPackage.getEntryFromOffset(bestType, bestOffset);
        if (bestEntry == 0) {
            return null;
        }

        return new FindEntryResult(
                bestCookie,
                bestEntry,
                0
        );
    }

    public PackAssets getPackAssets(int cookie) {
        if (cookie < 0 || cookie >= packAssets.length) {
            return null;
        }
        return packAssets[cookie];
    }

    private PackAssets[] packAssets;
    private final HashMap<String, PackageGroup> packageGroups = new HashMap<>();
}
