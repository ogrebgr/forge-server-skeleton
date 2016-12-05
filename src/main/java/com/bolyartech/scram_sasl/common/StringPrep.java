/**
 * Copyright 2011 Glenn Maynard
 * <p>
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bolyartech.scram_sasl.common;


import java.util.*;


/**
 * rfc3454 StringPrep, with an implementation of rfc4013 SASLPrep.
 * <p>
 * StringPrep case folding is unimplemented, as it's not required by SASLPrep.
 */
@SuppressWarnings({"WeakerAccess", "unused", "SpellCheckingInspection", "JavaDoc"})
public class StringPrep {
    /**
     * A representation of sets of character classes.
     */
    @SuppressWarnings({"SpellCheckingInspection", "JavaDoc"})
    static protected class CharClass {
        // Each character class is a set of [start,end] tuples; each tuple is represented
        // in the mapping as mapping[start] = (end-start+1).
        // Invariants:
        // - tupleStart is in ascending order.
        // - All values in tupleCount are >= 1 (no empty ranges).
        // - tupleStart.size() == tupleCount.size().
        // - There will be no overlapping ranges.
        //
        // TreeMap would work well for this, but it was missing basic operations like lowerEntry
        // until JDK1.6, which we don't want to depend on.
        private final ArrayList<Integer> tupleStart = new ArrayList<>();
        private final ArrayList<Integer> tupleCount = new ArrayList<>();


        @SuppressWarnings("ForLoopReplaceableByForEach")
        static CharClass fromList(int[] charMap) {
            SortedMap<Integer, Integer> mapping = new TreeMap<>();
            for (int i = 0; i < charMap.length; ++i)
                mapping.put(charMap[i], 1);

            return new CharClass(mapping);
        }


        static CharClass fromRanges(int[] charMap) {
            // There must be an even number of tuples in RANGES tables.
            if ((charMap.length % 2) != 0)
                throw new IllegalArgumentException("Invalid character list size");

            SortedMap<Integer, Integer> mapping = new TreeMap<>();
            for (int i = 0; i < charMap.length; i += 2) {
                int start = charMap[i];
                int end = charMap[i + 1];
                int count = end - start + 1;
                mapping.put(start, count);
            }

            return new CharClass(mapping);
        }


        static CharClass fromClasses(CharClass... classes) {
            SortedMap<Integer, Integer> mapping = new TreeMap<>();
            for (CharClass charClass : classes) {
                for (int i = 0; i < charClass.tupleStart.size(); ++i) {
                    int start = charClass.tupleStart.get(i);
                    int count = charClass.tupleCount.get(i);
                    mapping.put(start, count);
                }
            }

            return new CharClass(mapping);
        }


        private CharClass(SortedMap<Integer, Integer> mappings) {
            for (Map.Entry<Integer, Integer> pair : mappings.entrySet()) {
                int start = pair.getKey();
                int count = pair.getValue();

                // Coalesce overlapping ranges.
                if (tupleStart.size() > 0) {
                    int prevIndex = tupleStart.size() - 1;
                    int prevStart = tupleStart.get(prevIndex);
                    int prevCount = tupleCount.get(prevIndex);
                    // If the previous tuple is (0,1), and this tuple is (1,5), then
                    // coalesce into (0,6).  If ths previous tuple is (0,3) and this
                    // tuple is (1,3), coalesce into (0,4).
                    if (prevStart + prevCount >= start) {
                        int endPos = start + count;
                        int newCount = endPos - prevStart;
                        tupleCount.set(prevIndex, newCount);
                        continue;
                    }
                }

                tupleStart.add(start);
                tupleCount.add(count);
            }
        }


        public boolean isCharInClass(int c) {
            // Find the first entry in tupleStart which is <= c.  Java's binarySearch
            // API is a bit braindamaged (it was written only considering search-and-insert),
            // so we have to jump some hoops to get this.
            int pos = Collections.binarySearch(tupleStart, c);
            if (pos >= 0) {
                // If pos >= 0, tupleStart[pos] == c.  The value is the start of a range,
                // so it's included in the class.
                return true;
                //while(pos > 0 && tupleStart.get(pos-1) == c)
                //    --pos;
            }
            // -pos - 1 is the lowest index where tupleStart[pos] > c.  If this is the
            // first entry, then c is below all entries in the class.
            pos = -pos - 1;
            if (pos == 0)
                return false;
            --pos;

            // tupleStart[pos] is <= c.
            int start = tupleStart.get(pos);
            int count = tupleCount.get(pos);
            return start <= c && c < start + count;

        }
    }

    /** A.1 Unassigned code points in Unicode 3.2 */
    static final CharClass A1 = CharClass.fromRanges(new int[]{
            0x0221, 0x0221, 0x0234, 0x024F, 0x02AE, 0x02AF, 0x02EF, 0x02FF, 0x0350, 0x035F, 0x0370, 0x0373,
            0x0376, 0x0379, 0x037B, 0x037D, 0x037F, 0x0383, 0x038B, 0x038B, 0x038D, 0x038D, 0x03A2, 0x03A2,
            0x03CF, 0x03CF, 0x03F7, 0x03FF, 0x0487, 0x0487, 0x04CF, 0x04CF, 0x04F6, 0x04F7, 0x04FA, 0x04FF,
            0x0510, 0x0530, 0x0557, 0x0558, 0x0560, 0x0560, 0x0588, 0x0588, 0x058B, 0x0590, 0x05A2, 0x05A2,
            0x05BA, 0x05BA, 0x05C5, 0x05CF, 0x05EB, 0x05EF, 0x05F5, 0x060B, 0x060D, 0x061A, 0x061C, 0x061E,
            0x0620, 0x0620, 0x063B, 0x063F, 0x0656, 0x065F, 0x06EE, 0x06EF, 0x06FF, 0x06FF, 0x070E, 0x070E,
            0x072D, 0x072F, 0x074B, 0x077F, 0x07B2, 0x0900, 0x0904, 0x0904, 0x093A, 0x093B, 0x094E, 0x094F,
            0x0955, 0x0957, 0x0971, 0x0980, 0x0984, 0x0984, 0x098D, 0x098E, 0x0991, 0x0992, 0x09A9, 0x09A9,
            0x09B1, 0x09B1, 0x09B3, 0x09B5, 0x09BA, 0x09BB, 0x09BD, 0x09BD, 0x09C5, 0x09C6, 0x09C9, 0x09CA,
            0x09CE, 0x09D6, 0x09D8, 0x09DB, 0x09DE, 0x09DE, 0x09E4, 0x09E5, 0x09FB, 0x0A01, 0x0A03, 0x0A04,
            0x0A0B, 0x0A0E, 0x0A11, 0x0A12, 0x0A29, 0x0A29, 0x0A31, 0x0A31, 0x0A34, 0x0A34, 0x0A37, 0x0A37,
            0x0A3A, 0x0A3B, 0x0A3D, 0x0A3D, 0x0A43, 0x0A46, 0x0A49, 0x0A4A, 0x0A4E, 0x0A58, 0x0A5D, 0x0A5D,
            0x0A5F, 0x0A65, 0x0A75, 0x0A80, 0x0A84, 0x0A84, 0x0A8C, 0x0A8C, 0x0A8E, 0x0A8E, 0x0A92, 0x0A92,
            0x0AA9, 0x0AA9, 0x0AB1, 0x0AB1, 0x0AB4, 0x0AB4, 0x0ABA, 0x0ABB, 0x0AC6, 0x0AC6, 0x0ACA, 0x0ACA,
            0x0ACE, 0x0ACF, 0x0AD1, 0x0ADF, 0x0AE1, 0x0AE5, 0x0AF0, 0x0B00, 0x0B04, 0x0B04, 0x0B0D, 0x0B0E,
            0x0B11, 0x0B12, 0x0B29, 0x0B29, 0x0B31, 0x0B31, 0x0B34, 0x0B35, 0x0B3A, 0x0B3B, 0x0B44, 0x0B46,
            0x0B49, 0x0B4A, 0x0B4E, 0x0B55, 0x0B58, 0x0B5B, 0x0B5E, 0x0B5E, 0x0B62, 0x0B65, 0x0B71, 0x0B81,
            0x0B84, 0x0B84, 0x0B8B, 0x0B8D, 0x0B91, 0x0B91, 0x0B96, 0x0B98, 0x0B9B, 0x0B9B, 0x0B9D, 0x0B9D,
            0x0BA0, 0x0BA2, 0x0BA5, 0x0BA7, 0x0BAB, 0x0BAD, 0x0BB6, 0x0BB6, 0x0BBA, 0x0BBD, 0x0BC3, 0x0BC5,
            0x0BC9, 0x0BC9, 0x0BCE, 0x0BD6, 0x0BD8, 0x0BE6, 0x0BF3, 0x0C00, 0x0C04, 0x0C04, 0x0C0D, 0x0C0D,
            0x0C11, 0x0C11, 0x0C29, 0x0C29, 0x0C34, 0x0C34, 0x0C3A, 0x0C3D, 0x0C45, 0x0C45, 0x0C49, 0x0C49,
            0x0C4E, 0x0C54, 0x0C57, 0x0C5F, 0x0C62, 0x0C65, 0x0C70, 0x0C81, 0x0C84, 0x0C84, 0x0C8D, 0x0C8D,
            0x0C91, 0x0C91, 0x0CA9, 0x0CA9, 0x0CB4, 0x0CB4, 0x0CBA, 0x0CBD, 0x0CC5, 0x0CC5, 0x0CC9, 0x0CC9,
            0x0CCE, 0x0CD4, 0x0CD7, 0x0CDD, 0x0CDF, 0x0CDF, 0x0CE2, 0x0CE5, 0x0CF0, 0x0D01, 0x0D04, 0x0D04,
            0x0D0D, 0x0D0D, 0x0D11, 0x0D11, 0x0D29, 0x0D29, 0x0D3A, 0x0D3D, 0x0D44, 0x0D45, 0x0D49, 0x0D49,
            0x0D4E, 0x0D56, 0x0D58, 0x0D5F, 0x0D62, 0x0D65, 0x0D70, 0x0D81, 0x0D84, 0x0D84, 0x0D97, 0x0D99,
            0x0DB2, 0x0DB2, 0x0DBC, 0x0DBC, 0x0DBE, 0x0DBF, 0x0DC7, 0x0DC9, 0x0DCB, 0x0DCE, 0x0DD5, 0x0DD5,
            0x0DD7, 0x0DD7, 0x0DE0, 0x0DF1, 0x0DF5, 0x0E00, 0x0E3B, 0x0E3E, 0x0E5C, 0x0E80, 0x0E83, 0x0E83,
            0x0E85, 0x0E86, 0x0E89, 0x0E89, 0x0E8B, 0x0E8C, 0x0E8E, 0x0E93, 0x0E98, 0x0E98, 0x0EA0, 0x0EA0,
            0x0EA4, 0x0EA4, 0x0EA6, 0x0EA6, 0x0EA8, 0x0EA9, 0x0EAC, 0x0EAC, 0x0EBA, 0x0EBA, 0x0EBE, 0x0EBF,
            0x0EC5, 0x0EC5, 0x0EC7, 0x0EC7, 0x0ECE, 0x0ECF, 0x0EDA, 0x0EDB, 0x0EDE, 0x0EFF, 0x0F48, 0x0F48,
            0x0F6B, 0x0F70, 0x0F8C, 0x0F8F, 0x0F98, 0x0F98, 0x0FBD, 0x0FBD, 0x0FCD, 0x0FCE, 0x0FD0, 0x0FFF,
            0x1022, 0x1022, 0x1028, 0x1028, 0x102B, 0x102B, 0x1033, 0x1035, 0x103A, 0x103F, 0x105A, 0x109F,
            0x10C6, 0x10CF, 0x10F9, 0x10FA, 0x10FC, 0x10FF, 0x115A, 0x115E, 0x11A3, 0x11A7, 0x11FA, 0x11FF,
            0x1207, 0x1207, 0x1247, 0x1247, 0x1249, 0x1249, 0x124E, 0x124F, 0x1257, 0x1257, 0x1259, 0x1259,
            0x125E, 0x125F, 0x1287, 0x1287, 0x1289, 0x1289, 0x128E, 0x128F, 0x12AF, 0x12AF, 0x12B1, 0x12B1,
            0x12B6, 0x12B7, 0x12BF, 0x12BF, 0x12C1, 0x12C1, 0x12C6, 0x12C7, 0x12CF, 0x12CF, 0x12D7, 0x12D7,
            0x12EF, 0x12EF, 0x130F, 0x130F, 0x1311, 0x1311, 0x1316, 0x1317, 0x131F, 0x131F, 0x1347, 0x1347,
            0x135B, 0x1360, 0x137D, 0x139F, 0x13F5, 0x1400, 0x1677, 0x167F, 0x169D, 0x169F, 0x16F1, 0x16FF,
            0x170D, 0x170D, 0x1715, 0x171F, 0x1737, 0x173F, 0x1754, 0x175F, 0x176D, 0x176D, 0x1771, 0x1771,
            0x1774, 0x177F, 0x17DD, 0x17DF, 0x17EA, 0x17FF, 0x180F, 0x180F, 0x181A, 0x181F, 0x1878, 0x187F,
            0x18AA, 0x1DFF, 0x1E9C, 0x1E9F, 0x1EFA, 0x1EFF, 0x1F16, 0x1F17, 0x1F1E, 0x1F1F, 0x1F46, 0x1F47,
            0x1F4E, 0x1F4F, 0x1F58, 0x1F58, 0x1F5A, 0x1F5A, 0x1F5C, 0x1F5C, 0x1F5E, 0x1F5E, 0x1F7E, 0x1F7F,
            0x1FB5, 0x1FB5, 0x1FC5, 0x1FC5, 0x1FD4, 0x1FD5, 0x1FDC, 0x1FDC, 0x1FF0, 0x1FF1, 0x1FF5, 0x1FF5,
            0x1FFF, 0x1FFF, 0x2053, 0x2056, 0x2058, 0x205E, 0x2064, 0x2069, 0x2072, 0x2073, 0x208F, 0x209F,
            0x20B2, 0x20CF, 0x20EB, 0x20FF, 0x213B, 0x213C, 0x214C, 0x2152, 0x2184, 0x218F, 0x23CF, 0x23FF,
            0x2427, 0x243F, 0x244B, 0x245F, 0x24FF, 0x24FF, 0x2614, 0x2615, 0x2618, 0x2618, 0x267E, 0x267F,
            0x268A, 0x2700, 0x2705, 0x2705, 0x270A, 0x270B, 0x2728, 0x2728, 0x274C, 0x274C, 0x274E, 0x274E,
            0x2753, 0x2755, 0x2757, 0x2757, 0x275F, 0x2760, 0x2795, 0x2797, 0x27B0, 0x27B0, 0x27BF, 0x27CF,
            0x27EC, 0x27EF, 0x2B00, 0x2E7F, 0x2E9A, 0x2E9A, 0x2EF4, 0x2EFF, 0x2FD6, 0x2FEF, 0x2FFC, 0x2FFF,
            0x3040, 0x3040, 0x3097, 0x3098, 0x3100, 0x3104, 0x312D, 0x3130, 0x318F, 0x318F, 0x31B8, 0x31EF,
            0x321D, 0x321F, 0x3244, 0x3250, 0x327C, 0x327E, 0x32CC, 0x32CF, 0x32FF, 0x32FF, 0x3377, 0x337A,
            0x33DE, 0x33DF, 0x33FF, 0x33FF, 0x4DB6, 0x4DFF, 0x9FA6, 0x9FFF, 0xA48D, 0xA48F, 0xA4C7, 0xABFF,
            0xD7A4, 0xD7FF, 0xFA2E, 0xFA2F, 0xFA6B, 0xFAFF, 0xFB07, 0xFB12, 0xFB18, 0xFB1C, 0xFB37, 0xFB37,
            0xFB3D, 0xFB3D, 0xFB3F, 0xFB3F, 0xFB42, 0xFB42, 0xFB45, 0xFB45, 0xFBB2, 0xFBD2, 0xFD40, 0xFD4F,
            0xFD90, 0xFD91, 0xFDC8, 0xFDCF, 0xFDFD, 0xFDFF, 0xFE10, 0xFE1F, 0xFE24, 0xFE2F, 0xFE47, 0xFE48,
            0xFE53, 0xFE53, 0xFE67, 0xFE67, 0xFE6C, 0xFE6F, 0xFE75, 0xFE75, 0xFEFD, 0xFEFE, 0xFF00, 0xFF00,
            0xFFBF, 0xFFC1, 0xFFC8, 0xFFC9, 0xFFD0, 0xFFD1, 0xFFD8, 0xFFD9, 0xFFDD, 0xFFDF, 0xFFE7, 0xFFE7,
            0xFFEF, 0xFFF8,
            0x10000, 0x102FF, 0x1031F, 0x1031F, 0x10324, 0x1032F, 0x1034B, 0x103FF, 0x10426, 0x10427,
            0x1044E, 0x1CFFF, 0x1D0F6, 0x1D0FF, 0x1D127, 0x1D129, 0x1D1DE, 0x1D3FF, 0x1D455, 0x1D455,
            0x1D49D, 0x1D49D, 0x1D4A0, 0x1D4A1, 0x1D4A3, 0x1D4A4, 0x1D4A7, 0x1D4A8, 0x1D4AD, 0x1D4AD,
            0x1D4BA, 0x1D4BA, 0x1D4BC, 0x1D4BC, 0x1D4C1, 0x1D4C1, 0x1D4C4, 0x1D4C4, 0x1D506, 0x1D506,
            0x1D50B, 0x1D50C, 0x1D515, 0x1D515, 0x1D51D, 0x1D51D, 0x1D53A, 0x1D53A, 0x1D53F, 0x1D53F,
            0x1D545, 0x1D545, 0x1D547, 0x1D549, 0x1D551, 0x1D551, 0x1D6A4, 0x1D6A7, 0x1D7CA, 0x1D7CD,
            0x1D800, 0x1FFFD, 0x2A6D7, 0x2F7FF, 0x2FA1E, 0x2FFFD, 0x30000, 0x3FFFD, 0x40000, 0x4FFFD,
            0x50000, 0x5FFFD, 0x60000, 0x6FFFD, 0x70000, 0x7FFFD, 0x80000, 0x8FFFD, 0x90000, 0x9FFFD,
            0xA0000, 0xAFFFD, 0xB0000, 0xBFFFD, 0xC0000, 0xCFFFD, 0xD0000, 0xDFFFD, 0xE0000, 0xE0000,
            0xE0002, 0xE001F, 0xE0080, 0xEFFFD,
    });

    /** B.1 Commonly mapped to nothing */
    static final CharClass B1 = CharClass.fromList(new int[]{
            0x00AD, 0x034F, 0x1806, 0x180B, 0x180C, 0x180D, 0x200B, 0x200C, 0x200D, 0x2060,
            0xFE00, 0xFE01, 0xFE02, 0xFE03, 0xFE04, 0xFE05, 0xFE06, 0xFE07, 0xFE08, 0xFE09,
            0xFE0A, 0xFE0B, 0xFE0C, 0xFE0D, 0xFE0E, 0xFE0F, 0xFEFF,
    });

    /** C.1.1 ASCII space characters */
    static final CharClass C11 = CharClass.fromList(new int[]{
            0x0020
    });

    /** C.1.2 Non-ASCII space characters */
    static final CharClass C12 = CharClass.fromList(new int[]{
            0x00A0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007,
            0x2008, 0x2009, 0x200A, 0x200B, 0x202F, 0x205F, 0x3000,
    });

    /** C.2.1 ASCII control characters */
    static final CharClass C21 = CharClass.fromList(new int[]{
            0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007, 0x0008, 0x0009,
            0x000A, 0x000B, 0x000C, 0x000D, 0x000E, 0x000F, 0x0010, 0x0011, 0x0012, 0x0013,
            0x0014, 0x0015, 0x0016, 0x0017, 0x0018, 0x0019, 0x001A, 0x001B, 0x001C, 0x001D,
            0x001E, 0x001F, 0x007F
    });

    /** C.2.2 Non-ASCII control characters */
    static final CharClass C22 = CharClass.fromList(new int[]{
            0x0080, 0x0081, 0x0082, 0x0083, 0x0084, 0x0085, 0x0086, 0x0087, 0x0088, 0x0089,
            0x008A, 0x008B, 0x008C, 0x008D, 0x008E, 0x008F, 0x0090, 0x0091, 0x0092, 0x0093,
            0x0094, 0x0095, 0x0096, 0x0097, 0x0098, 0x0099, 0x009A, 0x009B, 0x009C, 0x009D,
            0x009E, 0x009F, 0x06DD, 0x070F, 0x180E, 0x200C, 0x200D, 0x2028, 0x2029, 0x2060,
            0x2061, 0x2062, 0x2063, 0x206A, 0x206B, 0x206C, 0x206D, 0x206E, 0x206F, 0xFEFF,
            0xFFF9, 0xFFFA, 0xFFFB, 0xFFFC,
            0x1D173, 0x1D174, 0x1D175, 0x1D176, 0x1D177, 0x1D178, 0x1D179, 0x1D17A,
    });

    /** C.3 Private use */
    static final CharClass C3 = CharClass.fromRanges(new int[]{
            0xE000, 0xF8FF, 0xF0000, 0xFFFFD, 0x100000, 0x10FFFD,
    });

    /** C.4 Non-character code points */
    static final CharClass C4 = CharClass.fromRanges(new int[]{
            0xFDD0, 0xFDEF, 0xFFFE, 0xFFFF, 0x1FFFE, 0x1FFFF, 0x2FFFE, 0x2FFFF,
            0x3FFFE, 0x3FFFF, 0x4FFFE, 0x4FFFF, 0x5FFFE, 0x5FFFF, 0x6FFFE, 0x6FFFF,
            0x7FFFE, 0x7FFFF, 0x8FFFE, 0x8FFFF, 0x9FFFE, 0x9FFFF, 0xAFFFE, 0xAFFFF,
            0xBFFFE, 0xBFFFF, 0xCFFFE, 0xCFFFF, 0xDFFFE, 0xDFFFF, 0xEFFFE, 0xEFFFF,
            0xFFFFE, 0xFFFFF, 0x10FFFE, 0x10FFFF,
    });

    /** C.5 Surrogate codes */
    static final CharClass C5 = CharClass.fromRanges(new int[]{
            0xD800, 0xDFFF,
    });

    /** C.6 Inappropriate for plain text */
    static final CharClass C6 = CharClass.fromList(new int[]{
            0xFFF9, 0xFFFA, 0xFFFB, 0xFFFC, 0xFFFD,
    });

    /** C.7 Inappropriate for canonical representation */
    static final CharClass C7 = CharClass.fromList(new int[]{
            0x2FF0, 0x2FF1, 0x2FF2, 0x2FF3, 0x2FF4, 0x2FF5, 0x2FF6, 0x2FF7, 0x2FF8, 0x2FF9,
            0x2FFA, 0x2FFB,
    });

    /** C.8 Change display properties or are deprecated */
    static final CharClass C8 = CharClass.fromList(new int[]{
            0x0340, 0x0341, 0x200E, 0x200F, 0x202A, 0x202B, 0x202C, 0x202D, 0x202E, 0x206A,
            0x206B, 0x206C, 0x206D, 0x206E, 0x206F,
    });

    /** C.9 Tagging characters (tuples) */
    static final CharClass C9 = CharClass.fromRanges(new int[]{
            0xE0001, 0xE0001, 0xE0020, 0xE007F,
    });

    /** D.1 Characters with bidirectional property "R" or "AL" */
    static final CharClass D1 = CharClass.fromRanges(new int[]{
            0x05BE, 0x05BE, 0x05C0, 0x05C0, 0x05C3, 0x05C3, 0x05D0, 0x05EA, 0x05F0, 0x05F4,
            0x061B, 0x061B, 0x061F, 0x061F, 0x0621, 0x063A, 0x0640, 0x064A, 0x066D, 0x066F,
            0x0671, 0x06D5, 0x06DD, 0x06DD, 0x06E5, 0x06E6, 0x06FA, 0x06FE, 0x0700, 0x070D,
            0x0710, 0x0710, 0x0712, 0x072C, 0x0780, 0x07A5, 0x07B1, 0x07B1, 0x200F, 0x200F,
            0xFB1D, 0xFB1D, 0xFB1F, 0xFB28, 0xFB2A, 0xFB36, 0xFB38, 0xFB3C, 0xFB3E, 0xFB3E,
            0xFB40, 0xFB41, 0xFB43, 0xFB44, 0xFB46, 0xFBB1, 0xFBD3, 0xFD3D, 0xFD50, 0xFD8F,
            0xFD92, 0xFDC7, 0xFDF0, 0xFDFC, 0xFE70, 0xFE74, 0xFE76, 0xFEFC,
    });

    /** D.2 Characters with bidirectional property "L" */
    static final CharClass D2 = CharClass.fromRanges(new int[]{
            0x0041, 0x005A, 0x0061, 0x007A, 0x00AA, 0x00AA, 0x00B5, 0x00B5, 0x00BA, 0x00BA, 0x00C0, 0x00D6,
            0x00D8, 0x00F6, 0x00F8, 0x0220, 0x0222, 0x0233, 0x0250, 0x02AD, 0x02B0, 0x02B8, 0x02BB, 0x02C1,
            0x02D0, 0x02D1, 0x02E0, 0x02E4, 0x02EE, 0x02EE, 0x037A, 0x037A, 0x0386, 0x0386, 0x0388, 0x038A,
            0x038C, 0x038C, 0x038E, 0x03A1, 0x03A3, 0x03CE, 0x03D0, 0x03F5, 0x0400, 0x0482, 0x048A, 0x04CE,
            0x04D0, 0x04F5, 0x04F8, 0x04F9, 0x0500, 0x050F, 0x0531, 0x0556, 0x0559, 0x055F, 0x0561, 0x0587,
            0x0589, 0x0589, 0x0903, 0x0903, 0x0905, 0x0939, 0x093D, 0x0940, 0x0949, 0x094C, 0x0950, 0x0950,
            0x0958, 0x0961, 0x0964, 0x0970, 0x0982, 0x0983, 0x0985, 0x098C, 0x098F, 0x0990, 0x0993, 0x09A8,
            0x09AA, 0x09B0, 0x09B2, 0x09B2, 0x09B6, 0x09B9, 0x09BE, 0x09C0, 0x09C7, 0x09C8, 0x09CB, 0x09CC,
            0x09D7, 0x09D7, 0x09DC, 0x09DD, 0x09DF, 0x09E1, 0x09E6, 0x09F1, 0x09F4, 0x09FA, 0x0A05, 0x0A0A,
            0x0A0F, 0x0A10, 0x0A13, 0x0A28, 0x0A2A, 0x0A30, 0x0A32, 0x0A33, 0x0A35, 0x0A36, 0x0A38, 0x0A39,
            0x0A3E, 0x0A40, 0x0A59, 0x0A5C, 0x0A5E, 0x0A5E, 0x0A66, 0x0A6F, 0x0A72, 0x0A74, 0x0A83, 0x0A83,
            0x0A85, 0x0A8B, 0x0A8D, 0x0A8D, 0x0A8F, 0x0A91, 0x0A93, 0x0AA8, 0x0AAA, 0x0AB0, 0x0AB2, 0x0AB3,
            0x0AB5, 0x0AB9, 0x0ABD, 0x0AC0, 0x0AC9, 0x0AC9, 0x0ACB, 0x0ACC, 0x0AD0, 0x0AD0, 0x0AE0, 0x0AE0,
            0x0AE6, 0x0AEF, 0x0B02, 0x0B03, 0x0B05, 0x0B0C, 0x0B0F, 0x0B10, 0x0B13, 0x0B28, 0x0B2A, 0x0B30,
            0x0B32, 0x0B33, 0x0B36, 0x0B39, 0x0B3D, 0x0B3E, 0x0B40, 0x0B40, 0x0B47, 0x0B48, 0x0B4B, 0x0B4C,
            0x0B57, 0x0B57, 0x0B5C, 0x0B5D, 0x0B5F, 0x0B61, 0x0B66, 0x0B70, 0x0B83, 0x0B83, 0x0B85, 0x0B8A,
            0x0B8E, 0x0B90, 0x0B92, 0x0B95, 0x0B99, 0x0B9A, 0x0B9C, 0x0B9C, 0x0B9E, 0x0B9F, 0x0BA3, 0x0BA4,
            0x0BA8, 0x0BAA, 0x0BAE, 0x0BB5, 0x0BB7, 0x0BB9, 0x0BBE, 0x0BBF, 0x0BC1, 0x0BC2, 0x0BC6, 0x0BC8,
            0x0BCA, 0x0BCC, 0x0BD7, 0x0BD7, 0x0BE7, 0x0BF2, 0x0C01, 0x0C03, 0x0C05, 0x0C0C, 0x0C0E, 0x0C10,
            0x0C12, 0x0C28, 0x0C2A, 0x0C33, 0x0C35, 0x0C39, 0x0C41, 0x0C44, 0x0C60, 0x0C61, 0x0C66, 0x0C6F,
            0x0C82, 0x0C83, 0x0C85, 0x0C8C, 0x0C8E, 0x0C90, 0x0C92, 0x0CA8, 0x0CAA, 0x0CB3, 0x0CB5, 0x0CB9,
            0x0CBE, 0x0CBE, 0x0CC0, 0x0CC4, 0x0CC7, 0x0CC8, 0x0CCA, 0x0CCB, 0x0CD5, 0x0CD6, 0x0CDE, 0x0CDE,
            0x0CE0, 0x0CE1, 0x0CE6, 0x0CEF, 0x0D02, 0x0D03, 0x0D05, 0x0D0C, 0x0D0E, 0x0D10, 0x0D12, 0x0D28,
            0x0D2A, 0x0D39, 0x0D3E, 0x0D40, 0x0D46, 0x0D48, 0x0D4A, 0x0D4C, 0x0D57, 0x0D57, 0x0D60, 0x0D61,
            0x0D66, 0x0D6F, 0x0D82, 0x0D83, 0x0D85, 0x0D96, 0x0D9A, 0x0DB1, 0x0DB3, 0x0DBB, 0x0DBD, 0x0DBD,
            0x0DC0, 0x0DC6, 0x0DCF, 0x0DD1, 0x0DD8, 0x0DDF, 0x0DF2, 0x0DF4, 0x0E01, 0x0E30, 0x0E32, 0x0E33,
            0x0E40, 0x0E46, 0x0E4F, 0x0E5B, 0x0E81, 0x0E82, 0x0E84, 0x0E84, 0x0E87, 0x0E88, 0x0E8A, 0x0E8A,
            0x0E8D, 0x0E8D, 0x0E94, 0x0E97, 0x0E99, 0x0E9F, 0x0EA1, 0x0EA3, 0x0EA5, 0x0EA5, 0x0EA7, 0x0EA7,
            0x0EAA, 0x0EAB, 0x0EAD, 0x0EB0, 0x0EB2, 0x0EB3, 0x0EBD, 0x0EBD, 0x0EC0, 0x0EC4, 0x0EC6, 0x0EC6,
            0x0ED0, 0x0ED9, 0x0EDC, 0x0EDD, 0x0F00, 0x0F17, 0x0F1A, 0x0F34, 0x0F36, 0x0F36, 0x0F38, 0x0F38,
            0x0F3E, 0x0F47, 0x0F49, 0x0F6A, 0x0F7F, 0x0F7F, 0x0F85, 0x0F85, 0x0F88, 0x0F8B, 0x0FBE, 0x0FC5,
            0x0FC7, 0x0FCC, 0x0FCF, 0x0FCF, 0x1000, 0x1021, 0x1023, 0x1027, 0x1029, 0x102A, 0x102C, 0x102C,
            0x1031, 0x1031, 0x1038, 0x1038, 0x1040, 0x1057, 0x10A0, 0x10C5, 0x10D0, 0x10F8, 0x10FB, 0x10FB,
            0x1100, 0x1159, 0x115F, 0x11A2, 0x11A8, 0x11F9, 0x1200, 0x1206, 0x1208, 0x1246, 0x1248, 0x1248,
            0x124A, 0x124D, 0x1250, 0x1256, 0x1258, 0x1258, 0x125A, 0x125D, 0x1260, 0x1286, 0x1288, 0x1288,
            0x128A, 0x128D, 0x1290, 0x12AE, 0x12B0, 0x12B0, 0x12B2, 0x12B5, 0x12B8, 0x12BE, 0x12C0, 0x12C0,
            0x12C2, 0x12C5, 0x12C8, 0x12CE, 0x12D0, 0x12D6, 0x12D8, 0x12EE, 0x12F0, 0x130E, 0x1310, 0x1310,
            0x1312, 0x1315, 0x1318, 0x131E, 0x1320, 0x1346, 0x1348, 0x135A, 0x1361, 0x137C, 0x13A0, 0x13F4,
            0x1401, 0x1676, 0x1681, 0x169A, 0x16A0, 0x16F0, 0x1700, 0x170C, 0x170E, 0x1711, 0x1720, 0x1731,
            0x1735, 0x1736, 0x1740, 0x1751, 0x1760, 0x176C, 0x176E, 0x1770, 0x1780, 0x17B6, 0x17BE, 0x17C5,
            0x17C7, 0x17C8, 0x17D4, 0x17DA, 0x17DC, 0x17DC, 0x17E0, 0x17E9, 0x1810, 0x1819, 0x1820, 0x1877,
            0x1880, 0x18A8, 0x1E00, 0x1E9B, 0x1EA0, 0x1EF9, 0x1F00, 0x1F15, 0x1F18, 0x1F1D, 0x1F20, 0x1F45,
            0x1F48, 0x1F4D, 0x1F50, 0x1F57, 0x1F59, 0x1F59, 0x1F5B, 0x1F5B, 0x1F5D, 0x1F5D, 0x1F5F, 0x1F7D,
            0x1F80, 0x1FB4, 0x1FB6, 0x1FBC, 0x1FBE, 0x1FBE, 0x1FC2, 0x1FC4, 0x1FC6, 0x1FCC, 0x1FD0, 0x1FD3,
            0x1FD6, 0x1FDB, 0x1FE0, 0x1FEC, 0x1FF2, 0x1FF4, 0x1FF6, 0x1FFC, 0x200E, 0x200E, 0x2071, 0x2071,
            0x207F, 0x207F, 0x2102, 0x2102, 0x2107, 0x2107, 0x210A, 0x2113, 0x2115, 0x2115, 0x2119, 0x211D,
            0x2124, 0x2124, 0x2126, 0x2126, 0x2128, 0x2128, 0x212A, 0x212D, 0x212F, 0x2131, 0x2133, 0x2139,
            0x213D, 0x213F, 0x2145, 0x2149, 0x2160, 0x2183, 0x2336, 0x237A, 0x2395, 0x2395, 0x249C, 0x24E9,
            0x3005, 0x3007, 0x3021, 0x3029, 0x3031, 0x3035, 0x3038, 0x303C, 0x3041, 0x3096, 0x309D, 0x309F,
            0x30A1, 0x30FA, 0x30FC, 0x30FF, 0x3105, 0x312C, 0x3131, 0x318E, 0x3190, 0x31B7, 0x31F0, 0x321C,
            0x3220, 0x3243, 0x3260, 0x327B, 0x327F, 0x32B0, 0x32C0, 0x32CB, 0x32D0, 0x32FE, 0x3300, 0x3376,
            0x337B, 0x33DD, 0x33E0, 0x33FE, 0x3400, 0x4DB5, 0x4E00, 0x9FA5, 0xA000, 0xA48C, 0xAC00, 0xD7A3,
            0xD800, 0xFA2D, 0xFA30, 0xFA6A, 0xFB00, 0xFB06, 0xFB13, 0xFB17, 0xFF21, 0xFF3A, 0xFF41, 0xFF5A,
            0xFF66, 0xFFBE, 0xFFC2, 0xFFC7, 0xFFCA, 0xFFCF, 0xFFD2, 0xFFD7, 0xFFDA, 0xFFDC,
            0x10300, 0x1031E, 0x10320, 0x10323, 0x10330, 0x1034A, 0x10400, 0x10425, 0x10428, 0x1044D,
            0x1D000, 0x1D0F5, 0x1D100, 0x1D126, 0x1D12A, 0x1D166, 0x1D16A, 0x1D172, 0x1D183, 0x1D184,
            0x1D18C, 0x1D1A9, 0x1D1AE, 0x1D1DD, 0x1D400, 0x1D454, 0x1D456, 0x1D49C, 0x1D49E, 0x1D49F,
            0x1D4A2, 0x1D4A2, 0x1D4A5, 0x1D4A6, 0x1D4A9, 0x1D4AC, 0x1D4AE, 0x1D4B9, 0x1D4BB, 0x1D4BB,
            0x1D4BD, 0x1D4C0, 0x1D4C2, 0x1D4C3, 0x1D4C5, 0x1D505, 0x1D507, 0x1D50A, 0x1D50D, 0x1D514,
            0x1D516, 0x1D51C, 0x1D51E, 0x1D539, 0x1D53B, 0x1D53E, 0x1D540, 0x1D544, 0x1D546, 0x1D546,
            0x1D54A, 0x1D550, 0x1D552, 0x1D6A3, 0x1D6A8, 0x1D7C9, 0x20000, 0x2A6D6, 0x2F800, 0x2FA1D,
            0xF0000, 0xFFFFD, 0x100000, 0x10FFFD,
    });

    /** rfc4013 2.3. Prohibited Output */
    static final CharClass saslProhibited = CharClass.fromClasses(C12, C21, C22, C3, C4, C5, C6, C7, C8, C9);

    /** A prohibited string has been passed to StringPrep. */
    @SuppressWarnings({"WeakerAccess", "JavaDoc"})
    static abstract public class StringPrepError extends Exception {
        protected StringPrepError(String message) {
            super(message);
        }
    }

    /** A prohibited character was detected. */
    @SuppressWarnings({"WeakerAccess", "JavaDoc"})
    static public class StringPrepProhibitedCharacter extends StringPrepError {
        StringPrepProhibitedCharacter() {
            super("String contains a prohibited character");
        }


        protected StringPrepProhibitedCharacter(String s) {
            super(s);
        }
    }

    /** A prohibited unassigned codepoint was detected. */
    @SuppressWarnings("JavaDoc")
    static public class StringPrepUnassignedCodepoint extends StringPrepProhibitedCharacter {
        StringPrepUnassignedCodepoint() {
            super("String contains an unassigned codepoint");
        }
    }

    /** RTL verification has failed, according to rfc3454 section 6. */
    @SuppressWarnings({"unused", "JavaDoc"})
    static public class StringPrepRTLError extends StringPrepError {
        StringPrepRTLError() {
            super("Invalid RTL string");
        }
    }

    static public class StringPrepRTLErrorBothRALandL extends StringPrepRTLError {
    }

    static public class StringPrepRTLErrorRALWithoutPrefix extends StringPrepRTLError {
    }

    static public class StringPrepRTLErrorRALWithoutSuffix extends StringPrepRTLError {
    }


    /** Replace each character of {@code s} which is in the {@link CharClass} mapFrom
     *  with the string {@code mapTo}. */
    static String applyMapTo(String s, CharClass mapFrom, String mapTo) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            int c = Character.codePointAt(s, i);
            int charCount = Character.charCount(c);
            if (mapFrom.isCharInClass(c))
                result.append(mapTo);
            else
                result.append(s, i, i + charCount);
            i += charCount;
        }

        return result.toString();
    }


    /** Return the first character index in s which is in {@link CharClass}, or -1 if
     * no character is in the class.*/
    static int containsCharacterInClass(String s, CharClass charClass) {
        for (int i = 0; i < s.length(); ) {
            int c = Character.codePointAt(s, i);
            if (charClass.isCharInClass(c))
                return i;

            i += Character.charCount(c);
        }
        return -1;
    }


    /** Perform RTL verification according to rfc3454 section 6.  On failure,
     *  throw a subclass of {@link StringPrepRTLError}. */
    protected static void verifyRTL(String s) throws StringPrepRTLError {
        int containsRAL = containsCharacterInClass(s, D1);
        if (containsRAL != -1) {
            // 2) If a string contains any RandALCat character, the string MUST NOT
            // contain any LCat character.
            int containsL = containsCharacterInClass(s, D2);
            if (containsL != -1)
                throw new StringPrepRTLErrorBothRALandL();
            // 3) If a string contains any RandALCat character, a RandALCat
            // character MUST be the first character of the string
            if (containsRAL != 0)
                throw new StringPrepRTLErrorRALWithoutPrefix();

            // ... and a RandALCat character MUST be the last character of the string.
            if (!D1.isCharInClass(s.charAt(s.length() - 1)))
                throw new StringPrepRTLErrorRALWithoutSuffix();
        }
    }


    /** Apply SASLPrep and return the result.  {@code} is treated as a stored string. */
    static public String prepAsStoredString(String s) throws StringPrepError {
        s = prepAsQueryString(s);

        // rfc3454: 7. Unassigned Code Points in Stringprep Profiles
        // Stored strings using the profile MUST NOT contain any unassigned code points.
        // rfc4013: 2.5. Unassigned Code Points
        // This profile specifies the [StringPrep, A.1] table as its list of unassigned
        // code points.
        int containsUnassignedCodepoint = containsCharacterInClass(s, A1);
        if (containsUnassignedCodepoint != -1)
            throw new StringPrepUnassignedCodepoint();
        return s;
    }


    /** Apply SASLPrep and return the result.  {@code} is treated as a query string. */
    static public String prepAsQueryString(String s) throws StringPrepError {
        // 1) Map
        // rfc4013: 2.1. Mapping
        // Note that applying the mapping this way works here because we only
        // map to nothing or space.  A StringPrep mapping that maps strings to
        // another string (eg. case folding) can't be applied sequentially like
        // this.
        s = applyMapTo(s, B1, "");
        s = applyMapTo(s, C12, " ");

        // 2) Normalize
        // rfc4013: 2.2. Normalization
        s = Normalizer.normalize(s);

        // 3) Prohibit
        int idx = containsCharacterInClass(s, saslProhibited);
        if (idx != -1)
            throw new StringPrepProhibitedCharacter();

        // 4) Check bidi
        verifyRTL(s);

        return s;
    }


    public static boolean isContainingProhibitedCharacters(String s) {
        int idx = containsCharacterInClass(s, saslProhibited);
        return idx != -1;
    }
}
