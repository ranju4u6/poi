/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.xssf.usermodel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Palette;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.openxml4j.exceptions.InvalidFormatException;
import org.openxml4j.opc.Package;
import org.openxml4j.opc.PackagePart;
import org.openxml4j.opc.PackagePartName;
import org.openxml4j.opc.PackageRelationshipTypes;
import org.openxml4j.opc.PackagingURIHelper;
import org.openxml4j.opc.TargetMode;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookView;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookViews;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;


public class XSSFWorkbook implements Workbook {

    private CTWorkbook workbook;
    
    private List<XSSFSheet> sheets = new LinkedList<XSSFSheet>();

    public XSSFWorkbook() {
        this.workbook = CTWorkbook.Factory.newInstance();
        CTBookViews bvs = this.workbook.addNewBookViews();
        CTBookView bv = bvs.addNewWorkbookView();
        bv.setActiveTab(0);
        this.workbook.addNewSheets();
    }
    
    protected CTWorkbook getWorkbook() {
        return this.workbook;
    }
    
    public int addPicture(byte[] pictureData, int format) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int addSSTString(String string) {
        // TODO Auto-generated method stub
        return 0;
    }

    public Sheet cloneSheet(int sheetNum) {
        XSSFSheet srcSheet = sheets.get(sheetNum);
        String srcName = getSheetName(sheetNum);
        if (srcSheet != null) {
            XSSFSheet clonedSheet = srcSheet.cloneSheet();

            sheets.add(clonedSheet);
            CTSheet newcts = this.workbook.getSheets().addNewSheet();
            newcts.set(clonedSheet.getSheet());
            
            int i = 1;
            while (true) {
                //Try and find the next sheet name that is unique
                String name = srcName;
                String index = Integer.toString(i++);
                if (name.length() + index.length() + 2 < 31) {
                    name = name + "("+index+")";
                } else {
                    name = name.substring(0, 31 - index.length() - 2) + "(" +index + ")";
                }

                //If the sheet name is unique, then set it otherwise move on to the next number.
                if (getSheetIndex(name) == -1) {
                    setSheetName(sheets.size() - 1, name);
                    break;
                }
            }
            return clonedSheet;
        }
        return null;
    }

    public CellStyle createCellStyle() {
        // TODO Auto-generated method stub
        return null;
    }

    public DataFormat createDataFormat() {
        // TODO Auto-generated method stub
        return null;
    }

    public Font createFont() {
        // TODO Auto-generated method stub
        return null;
    }

    public Name createName() {
        // TODO Auto-generated method stub
        return null;
    }

    public Sheet createSheet() {
        return createSheet(null);
    }

    public Sheet createSheet(String sheetname) {
        CTSheet sheet = workbook.getSheets().addNewSheet();
        if (sheetname != null) {
            sheet.setName(sheetname);
        }
        XSSFSheet wrapper = new XSSFSheet(sheet);
        this.sheets.add(wrapper);
        return wrapper;
    }

    public void dumpDrawingGroupRecords(boolean fat) {
        // TODO Auto-generated method stub

    }

    public Font findFont(short boldWeight, short color, short fontHeight, String name, boolean italic, boolean strikeout, short typeOffset, byte underline) {
        // TODO Auto-generated method stub
        return null;
    }

    public List getAllEmbeddedObjects() {
        // TODO Auto-generated method stub
        return null;
    }

    public List getAllPictures() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getBackupFlag() {
        // TODO Auto-generated method stub
        return false;
    }

    public byte[] getBytes() {
        // TODO Auto-generated method stub
        return null;
    }

    public CellStyle getCellStyleAt(short idx) {
        // TODO Auto-generated method stub
        return null;
    }

    public Palette getCustomPalette() {
        // TODO Auto-generated method stub
        return null;
    }

    public short getDisplayedTab() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Font getFontAt(short idx) {
        // TODO Auto-generated method stub
        return null;
    }

    public Name getNameAt(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    public int getNameIndex(String name) {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getNameName(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    public short getNumCellStyles() {
        // TODO Auto-generated method stub
        return 0;
    }

    public short getNumberOfFonts() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getNumberOfNames() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getNumberOfSheets() {
        return this.workbook.getSheets().sizeOfSheetArray();
    }

    public String getPrintArea(int sheetIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSSTString(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    public short getSelectedTab() {
        short i = 0;
        for (XSSFSheet sheet : this.sheets) {
            if (sheet.isTabSelected()) {
                return i;
            }
            ++i;
        }
        return -1;
    }

    public Sheet getSheet(String name) {
        CTSheet[] sheets = this.workbook.getSheets().getSheetArray();  
        for (int i = 0 ; i < sheets.length ; ++i) {
            if (name.equals(sheets[i].getName())) {
                return this.sheets.get(i);
            }
        }
        return null;
    }

    public Sheet getSheetAt(int index) {
        return this.sheets.get(index);
    }

    public int getSheetIndex(String name) {
        CTSheet[] sheets = this.workbook.getSheets().getSheetArray();  
        for (int i = 0 ; i < sheets.length ; ++i) {
            if (name.equals(sheets[i].getName())) {
                return i;
            }
        }
        return -1;
    }

    public int getSheetIndex(Sheet sheet) {
        return this.sheets.indexOf(sheet);
    }

    public String getSheetName(int sheet) {
        return this.workbook.getSheets().getSheetArray(sheet).getName();
    }

    public void insertChartRecord() {
        // TODO Auto-generated method stub

    }

    public void removeName(int index) {
        // TODO Auto-generated method stub

    }

    public void removeName(String name) {
        // TODO Auto-generated method stub

    }

    public void removePrintArea(int sheetIndex) {
        // TODO Auto-generated method stub

    }

    public void removeSheetAt(int index) {
        XSSFSheet sheet = this.sheets.remove(index);
        this.workbook.getSheets().removeSheet(index);
    }

    public void setBackupFlag(boolean backupValue) {
        // TODO Auto-generated method stub

    }

    public void setDisplayedTab(short index) {
        // TODO Auto-generated method stub

    }

    public void setPrintArea(int sheetIndex, String reference) {
        // TODO Auto-generated method stub

    }

    public void setPrintArea(int sheetIndex, int startColumn, int endColumn, int startRow, int endRow) {
        // TODO Auto-generated method stub

    }

    public void setRepeatingRowsAndColumns(int sheetIndex, int startColumn, int endColumn, int startRow, int endRow) {
        // TODO Auto-generated method stub

    }

    /**
     * We only set one sheet as selected for compatibility with HSSF.
     */
    public void setSelectedTab(short index) {
        for (int i = 0 ; i < this.sheets.size() ; ++i) {
            XSSFSheet sheet = this.sheets.get(i);
            sheet.setTabSelected(i == index);
        }
    }

    public void setSheetName(int sheet, String name) {
        this.workbook.getSheets().getSheetArray(sheet).setName(name);
    }

    public void setSheetName(int sheet, String name, short encoding) {
        this.workbook.getSheets().getSheetArray(sheet).setName(name);
    }

    public void setSheetOrder(String sheetname, int pos) {
        int idx = getSheetIndex(sheetname);
        sheets.add(pos, sheets.remove(idx));
        // Reorder CTSheets
        XmlObject cts = this.workbook.getSheets().getSheetArray(idx).copy();
        this.workbook.getSheets().removeSheet(idx);
        CTSheet newcts = this.workbook.getSheets().insertNewSheet(pos);
        newcts.set(cts);
    }

    public void unwriteProtectWorkbook() {
        // TODO Auto-generated method stub

    }

    public void write(OutputStream stream) throws IOException {

        try {
            // Create a package referring the temp file.
            Package pkg = Package.create(stream);
            // Main part
            PackagePartName corePartName = PackagingURIHelper.createPartName("/xl/workbook.xml");
            // Create main part relationship
            pkg.addRelationship(corePartName, TargetMode.INTERNAL, PackageRelationshipTypes.CORE_DOCUMENT, "rId1");
            // Create main document part
            PackagePart corePart = pkg.createPart(corePartName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml");
            XmlOptions xmlOptions = new XmlOptions();
             // Requests use of whitespace for easier reading
             xmlOptions.setSavePrettyPrint();
             xmlOptions.setSaveOuter();
             // XXX This should not be needed, but apparently the setSaveOuter call above does not work in XMLBeans 2.2
             xmlOptions.setSaveSyntheticDocumentElement(new QName(CTWorkbook.type.getName().getNamespaceURI(), "workbook"));
             xmlOptions.setUseDefaultNamespace();
             
             OutputStream out = corePart.getOutputStream();
             workbook.save(out, xmlOptions);
             out.close();
             
             for (int i = 0 ; i < this.getNumberOfSheets() ; ++i) {
                 XSSFSheet sheet = (XSSFSheet) this.getSheetAt(i);
                 PackagePartName partName = PackagingURIHelper.createPartName("/xl/worksheets/sheet" + i + ".xml");
                 corePart.addRelationship(partName, TargetMode.INTERNAL, "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet", "rSheet" + 1);
                 PackagePart part = pkg.createPart(partName, 
                         "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml");
                 
                 // XXX This should not be needed, but apparently the setSaveOuter call above does not work in XMLBeans 2.2
                 xmlOptions.setSaveSyntheticDocumentElement(new QName(CTWorksheet.type.getName().getNamespaceURI(), "worksheet"));
                 out = part.getOutputStream();
                 sheet.getWorksheet().save(out, xmlOptions);
                 
                 out.close();
             }
             
             pkg.close();
             
        } catch (InvalidFormatException e) {
            // TODO: replace with more meaningful exception
            throw new RuntimeException(e);
        }
    }

    public void writeProtectWorkbook(String password, String username) {
        // TODO Auto-generated method stub

    }

}
