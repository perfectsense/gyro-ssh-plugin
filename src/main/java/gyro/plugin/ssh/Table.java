/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.plugin.ssh;

import java.util.Iterator;
import java.util.Map;

import com.psddev.dari.util.CompactMap;
import gyro.core.GyroUI;

public class Table {

    private final Map<String, Integer> columns = new CompactMap<String, Integer>();

    public Table addColumn(String header, int width) {
        columns.put(header, width);
        return this;
    }

    private void writeSeparator(GyroUI ui, char first, char fill, char between, char last) {
        ui.write("%c%c", first, fill);

        for (Iterator<Integer> i = columns.values().iterator(); i.hasNext();) {
            int width = i.next();

            for (; width > 0; -- width) {
                ui.write("%c", fill);
            }

            if (i.hasNext()) {
                ui.write("%c%c%c", fill, between, fill);
            }
        }

        ui.write("%c%c\n", fill, last);
    }

    public void writeHeader(GyroUI ui) {

        writeSeparator(ui, '+', '-', '+', '+');
        ui.write("| ");

        for (Iterator<Map.Entry<String, Integer>> i = columns.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, Integer> entry = i.next();
            String header = entry.getKey();
            int remainder = entry.getValue() - header.length();

            if (remainder < 0) {
                header = header.substring(header.length() + remainder);
                remainder = 0;
            }

            ui.write(header);

            for (; remainder > 0; -- remainder) {
                ui.write(" ");
            }

            if (i.hasNext()) {
                ui.write(" | ");
            }
        }

        ui.write(" |\n");
        writeSeparator(ui, '+', '-', '+', '+');
    }

    public void writeRow(GyroUI ui, Object... cells) {
        ui.write("| ");

        int cellsLength = cells != null ? cells.length : 0;
        int index = 0;

        for (Iterator<Integer> i = columns.values().iterator(); i.hasNext(); ++ index) {
            int width = i.next();
            Object cell = index < cellsLength ? cells[index] : null;
            String cellString = cell != null ? cell.toString() : "";
            int remainder = width - cellString.length();

            ui.write(cellString);

            for (; remainder > 0; -- remainder) {
                ui.write(" ");
            }

            if (i.hasNext()) {
                ui.write(" | ");
            }
        }

        ui.write(" |\n");
    }

    public void writeFooter(GyroUI ui) {
        writeSeparator(ui, '+', '-', '+', '+');
    }
}
