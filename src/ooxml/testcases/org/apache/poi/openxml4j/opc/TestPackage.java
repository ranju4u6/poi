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

package org.apache.poi.openxml4j.opc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.poi.POIXMLException;
import org.apache.poi.openxml4j.OpenXML4JTestDataSamples;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.opc.internal.ContentTypeManager;
import org.apache.poi.openxml4j.opc.internal.FileHelper;
import org.apache.poi.openxml4j.opc.internal.PackagePropertiesPart;
import org.apache.poi.openxml4j.opc.internal.ZipHelper;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.DocumentHelper;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.TempFile;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class TestPackage {
    private static final POILogger logger = POILogFactory.getLogger(TestPackage.class);

	/**
	 * Test that just opening and closing the file doesn't alter the document.
	 */
    @Test
	public void openSave() throws Exception {
		String originalFile = OpenXML4JTestDataSamples.getSampleFileName("TestPackageCommon.docx");
		File targetFile = OpenXML4JTestDataSamples.getOutputFile("TestPackageOpenSaveTMP.docx");

		OPCPackage p = OPCPackage.open(originalFile, PackageAccess.READ_WRITE);
		try {
    		p.save(targetFile.getAbsoluteFile());
    
    		// Compare the original and newly saved document
    		assertTrue(targetFile.exists());
    		ZipFileAssert.assertEquals(new File(originalFile), targetFile);
    		assertTrue(targetFile.delete());
		} finally {
            // use revert to not re-write the input file
            p.revert();
		}
	}

	/**
	 * Test that when we create a new Package, we give it
	 *  the correct default content types
	 */
    @Test
	public void createGetsContentTypes() throws Exception {
		File targetFile = OpenXML4JTestDataSamples.getOutputFile("TestCreatePackageTMP.docx");

		// Zap the target file, in case of an earlier run
		if(targetFile.exists()) targetFile.delete();

		OPCPackage pkg = OPCPackage.create(targetFile);

		// Check it has content types for rels and xml
		ContentTypeManager ctm = getContentTypeManager(pkg);
		assertEquals(
				"application/xml",
				ctm.getContentType(
						PackagingURIHelper.createPartName("/foo.xml")
				)
		);
		assertEquals(
				ContentTypes.RELATIONSHIPS_PART,
				ctm.getContentType(
						PackagingURIHelper.createPartName("/foo.rels")
				)
		);
		assertNull(
				ctm.getContentType(
						PackagingURIHelper.createPartName("/foo.txt")
				)
		);
	}

	/**
	 * Test package creation.
	 */
    @Test
	public void createPackageAddPart() throws Exception {
		File targetFile = OpenXML4JTestDataSamples.getOutputFile("TestCreatePackageTMP.docx");

		File expectedFile = OpenXML4JTestDataSamples.getSampleFile("TestCreatePackageOUTPUT.docx");

        // Zap the target file, in case of an earlier run
        if(targetFile.exists()) targetFile.delete();

        // Create a package
        OPCPackage pkg = OPCPackage.create(targetFile);
        PackagePartName corePartName = PackagingURIHelper
                .createPartName("/word/document.xml");

        pkg.addRelationship(corePartName, TargetMode.INTERNAL,
                PackageRelationshipTypes.CORE_DOCUMENT, "rId1");

        PackagePart corePart = pkg
                .createPart(
                        corePartName,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml");

        Document doc = DocumentHelper.createDocument();
        Element elDocument = doc.createElementNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "w:document");
        doc.appendChild(elDocument);
        Element elBody = doc.createElementNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "w:body");
        elDocument.appendChild(elBody);
        Element elParagraph = doc.createElementNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "w:p");
        elBody.appendChild(elParagraph);
        Element elRun = doc.createElementNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "w:r");
        elParagraph.appendChild(elRun);
        Element elText = doc.createElementNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "w:t");
        elRun.appendChild(elText);
        elText.setTextContent("Hello Open XML !");

        StreamHelper.saveXmlInStream(doc, corePart.getOutputStream());
        pkg.close();

        ZipFileAssert.assertEquals(expectedFile, targetFile);
        assertTrue(targetFile.delete());
	}

	/**
	 * Tests that we can create a new package, add a core
	 *  document and another part, save and re-load and
	 *  have everything setup as expected
	 */
    @Test
	public void createPackageWithCoreDocument() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OPCPackage pkg = OPCPackage.create(baos);

		// Add a core document
        PackagePartName corePartName = PackagingURIHelper.createPartName("/xl/workbook.xml");
        // Create main part relationship
        pkg.addRelationship(corePartName, TargetMode.INTERNAL, PackageRelationshipTypes.CORE_DOCUMENT, "rId1");
        // Create main document part
        PackagePart corePart = pkg.createPart(corePartName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml");
        // Put in some dummy content
        OutputStream coreOut = corePart.getOutputStream();
        coreOut.write("<dummy-xml />".getBytes());
        coreOut.close();

		// And another bit
        PackagePartName sheetPartName = PackagingURIHelper.createPartName("/xl/worksheets/sheet1.xml");
        PackageRelationship rel =
        	 corePart.addRelationship(sheetPartName, TargetMode.INTERNAL, "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet", "rSheet1");
        PackagePart part = pkg.createPart(sheetPartName, "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml");
        assertNotNull(part);

        // Dummy content again
        coreOut = corePart.getOutputStream();
        coreOut.write("<dummy-xml2 />".getBytes());
        coreOut.close();

        //add a relationship with internal target: "#Sheet1!A1"
        corePart.addRelationship(new URI("#Sheet1!A1"), TargetMode.INTERNAL, "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink", "rId2");

        // Check things are as expected
        PackageRelationshipCollection coreRels =
        	pkg.getRelationshipsByType(PackageRelationshipTypes.CORE_DOCUMENT);
        assertEquals(1, coreRels.size());
        PackageRelationship coreRel = coreRels.getRelationship(0);
        assertEquals("/", coreRel.getSourceURI().toString());
        assertEquals("/xl/workbook.xml", coreRel.getTargetURI().toString());
        assertNotNull(pkg.getPart(coreRel));


        // Save and re-load
        pkg.close();
        File tmp = TempFile.createTempFile("testCreatePackageWithCoreDocument", ".zip");
        OutputStream fout = new FileOutputStream(tmp);
        try {
            fout.write(baos.toByteArray());
        } finally {
            fout.close();
        }
        pkg = OPCPackage.open(tmp.getPath());
        //tmp.delete();

        try {
            // Check still right
            coreRels = pkg.getRelationshipsByType(PackageRelationshipTypes.CORE_DOCUMENT);
            assertEquals(1, coreRels.size());
            coreRel = coreRels.getRelationship(0);
    
            assertEquals("/", coreRel.getSourceURI().toString());
            assertEquals("/xl/workbook.xml", coreRel.getTargetURI().toString());
            corePart = pkg.getPart(coreRel);
            assertNotNull(corePart);
    
            PackageRelationshipCollection rels = corePart.getRelationshipsByType("http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink");
            assertEquals(1, rels.size());
            rel = rels.getRelationship(0);
            assertEquals("Sheet1!A1", rel.getTargetURI().getRawFragment());
    
            assertMSCompatibility(pkg);
        } finally {
            pkg.close();
        }
    }

    private void assertMSCompatibility(OPCPackage pkg) throws Exception {
        PackagePartName relName = PackagingURIHelper.createPartName(PackageRelationship.getContainerPartRelationship());
        PackagePart relPart = pkg.getPart(relName);

        Document xmlRelationshipsDoc = DocumentHelper.readDocument(relPart.getInputStream());

        Element root = xmlRelationshipsDoc.getDocumentElement();
        NodeList nodeList = root.getElementsByTagName(PackageRelationship.RELATIONSHIP_TAG_NAME);
        int nodeCount = nodeList.getLength();
        for (int i = 0; i < nodeCount; i++) {
            Element element = (Element) nodeList.item(i);
            String value = element.getAttribute(PackageRelationship.TARGET_ATTRIBUTE_NAME);
            assertTrue("Root target must not start with a leading slash ('/'): " + value, value.charAt(0) != '/');
        }

    }

    /**
	 * Test package opening.
	 */
    @Test
	public void openPackage() throws Exception {
		File targetFile = OpenXML4JTestDataSamples.getOutputFile("TestOpenPackageTMP.docx");

		File inputFile = OpenXML4JTestDataSamples.getSampleFile("TestOpenPackageINPUT.docx");

		File expectedFile = OpenXML4JTestDataSamples.getSampleFile("TestOpenPackageOUTPUT.docx");

		// Copy the input file in the output directory
		FileHelper.copyFile(inputFile, targetFile);

		// Create a package
		OPCPackage pkg = OPCPackage.open(targetFile.getAbsolutePath());

		// Modify core part
		PackagePartName corePartName = PackagingURIHelper
				.createPartName("/word/document.xml");

		PackagePart corePart = pkg.getPart(corePartName);

		// Delete some part to have a valid document
		for (PackageRelationship rel : corePart.getRelationships()) {
			corePart.removeRelationship(rel.getId());
			pkg.removePart(PackagingURIHelper.createPartName(PackagingURIHelper
					.resolvePartUri(corePart.getPartName().getURI(), rel
							.getTargetURI())));
		}

		// Create a content
		Document doc = DocumentHelper.createDocument();
        Element elDocument = doc.createElementNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "w:document");
        doc.appendChild(elDocument);
        Element elBody = doc.createElementNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "w:body");
        elDocument.appendChild(elBody);
        Element elParagraph = doc.createElementNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "w:p");
        elBody.appendChild(elParagraph);
        Element elRun = doc.createElementNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "w:r");
        elParagraph.appendChild(elRun);
        Element elText = doc.createElementNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "w:t");
        elRun.appendChild(elText);
        elText.setTextContent("Hello Open XML !");

		StreamHelper.saveXmlInStream(doc, corePart.getOutputStream());

		// Save and close
		try {
			pkg.close();
		} catch (IOException e) {
			fail();
		}

		ZipFileAssert.assertEquals(expectedFile, targetFile);
		assertTrue(targetFile.delete());
	}

	/**
	 * Checks that we can write a package to a simple
	 *  OutputStream, in addition to the normal writing
	 *  to a file
	 */
    @Test
	public void saveToOutputStream() throws Exception {
		String originalFile = OpenXML4JTestDataSamples.getSampleFileName("TestPackageCommon.docx");
		File targetFile = OpenXML4JTestDataSamples.getOutputFile("TestPackageOpenSaveTMP.docx");

		OPCPackage p = OPCPackage.open(originalFile, PackageAccess.READ_WRITE);
		try {
    		FileOutputStream fout = new FileOutputStream(targetFile);
    		try {
    		    p.save(fout);
    		} finally {
    		    fout.close();
    		}
    
    		// Compare the original and newly saved document
    		assertTrue(targetFile.exists());
    		ZipFileAssert.assertEquals(new File(originalFile), targetFile);
    		assertTrue(targetFile.delete());
		} finally {
		    // use revert to not re-write the input file
		    p.revert();
		}
	}

	/**
	 * Checks that we can open+read a package from a
	 *  simple InputStream, in addition to the normal
	 *  reading from a file
	 */
    @Test
	public void openFromInputStream() throws Exception {
		String originalFile = OpenXML4JTestDataSamples.getSampleFileName("TestPackageCommon.docx");

		FileInputStream finp = new FileInputStream(originalFile);

		OPCPackage p = OPCPackage.open(finp);

		assertNotNull(p);
		assertNotNull(p.getRelationships());
		assertEquals(12, p.getParts().size());

		// Check it has the usual bits
		assertTrue(p.hasRelationships());
		assertTrue(p.containPart(PackagingURIHelper.createPartName("/_rels/.rels")));
	}

    /**
     * TODO: fix and enable
     */
    @Test
    @Ignore
    public void removePartRecursive() throws Exception {
		String originalFile = OpenXML4JTestDataSamples.getSampleFileName("TestPackageCommon.docx");
		File targetFile = OpenXML4JTestDataSamples.getOutputFile("TestPackageRemovePartRecursiveOUTPUT.docx");
		File tempFile = OpenXML4JTestDataSamples.getOutputFile("TestPackageRemovePartRecursiveTMP.docx");

		OPCPackage p = OPCPackage.open(originalFile, PackageAccess.READ_WRITE);
		p.removePartRecursive(PackagingURIHelper.createPartName(new URI(
				"/word/document.xml")));
		p.save(tempFile.getAbsoluteFile());

		// Compare the original and newly saved document
		assertTrue(targetFile.exists());
		ZipFileAssert.assertEquals(targetFile, tempFile);
		assertTrue(targetFile.delete());
	}

    @Test
	public void deletePart() throws InvalidFormatException {
		TreeMap<PackagePartName, String> expectedValues;
		TreeMap<PackagePartName, String> values;

		values = new TreeMap<PackagePartName, String>();

		// Expected values
		expectedValues = new TreeMap<PackagePartName, String>();
		expectedValues.put(PackagingURIHelper.createPartName("/_rels/.rels"),
				"application/vnd.openxmlformats-package.relationships+xml");

		expectedValues
				.put(PackagingURIHelper.createPartName("/docProps/app.xml"),
						"application/vnd.openxmlformats-officedocument.extended-properties+xml");
		expectedValues.put(PackagingURIHelper
				.createPartName("/docProps/core.xml"),
				"application/vnd.openxmlformats-package.core-properties+xml");
		expectedValues
				.put(PackagingURIHelper.createPartName("/word/fontTable.xml"),
						"application/vnd.openxmlformats-officedocument.wordprocessingml.fontTable+xml");
		expectedValues.put(PackagingURIHelper
				.createPartName("/word/media/image1.gif"), "image/gif");
		expectedValues
				.put(PackagingURIHelper.createPartName("/word/settings.xml"),
						"application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml");
		expectedValues
				.put(PackagingURIHelper.createPartName("/word/styles.xml"),
						"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml");
		expectedValues.put(PackagingURIHelper
				.createPartName("/word/theme/theme1.xml"),
				"application/vnd.openxmlformats-officedocument.theme+xml");
		expectedValues
				.put(
						PackagingURIHelper
								.createPartName("/word/webSettings.xml"),
						"application/vnd.openxmlformats-officedocument.wordprocessingml.webSettings+xml");

		String filepath =  OpenXML4JTestDataSamples.getSampleFileName("sample.docx");

		OPCPackage p = OPCPackage.open(filepath, PackageAccess.READ_WRITE);
		// Remove the core part
		p.deletePart(PackagingURIHelper.createPartName("/word/document.xml"));

		for (PackagePart part : p.getParts()) {
			values.put(part.getPartName(), part.getContentType());
			logger.log(POILogger.DEBUG, part.getPartName());
		}

		// Compare expected values with values return by the package
		for (PackagePartName partName : expectedValues.keySet()) {
			assertNotNull(values.get(partName));
			assertEquals(expectedValues.get(partName), values.get(partName));
		}
		// Don't save modifications
		p.revert();
	}

    @Test
	public void deletePartRecursive() throws InvalidFormatException {
		TreeMap<PackagePartName, String> expectedValues;
		TreeMap<PackagePartName, String> values;

		values = new TreeMap<PackagePartName, String>();

		// Expected values
		expectedValues = new TreeMap<PackagePartName, String>();
		expectedValues.put(PackagingURIHelper.createPartName("/_rels/.rels"),
				"application/vnd.openxmlformats-package.relationships+xml");

		expectedValues
				.put(PackagingURIHelper.createPartName("/docProps/app.xml"),
						"application/vnd.openxmlformats-officedocument.extended-properties+xml");
		expectedValues.put(PackagingURIHelper
				.createPartName("/docProps/core.xml"),
				"application/vnd.openxmlformats-package.core-properties+xml");

		String filepath = OpenXML4JTestDataSamples.getSampleFileName("sample.docx");

		OPCPackage p = OPCPackage.open(filepath, PackageAccess.READ_WRITE);
		// Remove the core part
		p.deletePartRecursive(PackagingURIHelper.createPartName("/word/document.xml"));

		for (PackagePart part : p.getParts()) {
			values.put(part.getPartName(), part.getContentType());
			logger.log(POILogger.DEBUG, part.getPartName());
		}

		// Compare expected values with values return by the package
		for (PackagePartName partName : expectedValues.keySet()) {
			assertNotNull(values.get(partName));
			assertEquals(expectedValues.get(partName), values.get(partName));
		}
		// Don't save modifications
		p.revert();
	}
	
	/**
	 * Test that we can open a file by path, and then
	 *  write changes to it.
	 */
    @Test
	public void openFileThenOverwrite() throws Exception {
        File tempFile = TempFile.createTempFile("poiTesting","tmp");
        File origFile = OpenXML4JTestDataSamples.getSampleFile("TestPackageCommon.docx");
        FileHelper.copyFile(origFile, tempFile);
        
        // Open the temp file
        OPCPackage p = OPCPackage.open(tempFile.toString(), PackageAccess.READ_WRITE);
        // Close it
        p.close();
        // Delete it
        assertTrue(tempFile.delete());
        
        // Reset
        FileHelper.copyFile(origFile, tempFile);
        p = OPCPackage.open(tempFile.toString(), PackageAccess.READ_WRITE);
        
        // Save it to the same file - not allowed
        try {
            p.save(tempFile);
            fail("You shouldn't be able to call save(File) to overwrite the current file");
        } catch(InvalidOperationException e) {}

        p.close();
        // Delete it
        assertTrue(tempFile.delete());
        
        
        // Open it read only, then close and delete - allowed
        FileHelper.copyFile(origFile, tempFile);
        p = OPCPackage.open(tempFile.toString(), PackageAccess.READ);
        p.close();
        assertTrue(tempFile.delete());
	}
    /**
     * Test that we can open a file by path, save it
     *  to another file, then delete both
     */
    @Test
    public void openFileThenSaveDelete() throws Exception {
        File tempFile = TempFile.createTempFile("poiTesting","tmp");
        File tempFile2 = TempFile.createTempFile("poiTesting","tmp");
        File origFile = OpenXML4JTestDataSamples.getSampleFile("TestPackageCommon.docx");
        FileHelper.copyFile(origFile, tempFile);
        
        // Open the temp file
        OPCPackage p = OPCPackage.open(tempFile.toString(), PackageAccess.READ_WRITE);

        // Save it to a different file
        p.save(tempFile2);
        p.close();
        
        // Delete both the files
        assertTrue(tempFile.delete());
        assertTrue(tempFile2.delete());
    }

	private static ContentTypeManager getContentTypeManager(OPCPackage pkg) throws Exception {
		Field f = OPCPackage.class.getDeclaredField("contentTypeManager");
		f.setAccessible(true);
		return (ContentTypeManager)f.get(pkg);
	}

    @Test
    public void getPartsByName() throws Exception {
        String filepath =  OpenXML4JTestDataSamples.getSampleFileName("sample.docx");

        OPCPackage pkg = OPCPackage.open(filepath, PackageAccess.READ_WRITE);
        try {
            List<PackagePart> rs =  pkg.getPartsByName(Pattern.compile("/word/.*?\\.xml"));
            HashMap<String, PackagePart>  selected = new HashMap<String, PackagePart>();
    
            for(PackagePart p : rs)
                selected.put(p.getPartName().getName(), p);
    
            assertEquals(6, selected.size());
            assertTrue(selected.containsKey("/word/document.xml"));
            assertTrue(selected.containsKey("/word/fontTable.xml"));
            assertTrue(selected.containsKey("/word/settings.xml"));
            assertTrue(selected.containsKey("/word/styles.xml"));
            assertTrue(selected.containsKey("/word/theme/theme1.xml"));
            assertTrue(selected.containsKey("/word/webSettings.xml"));
        } finally {
            // use revert to not re-write the input file
            pkg.revert();
        }
    }
    
    @Test
    public void getPartSize() throws Exception {
       String filepath =  OpenXML4JTestDataSamples.getSampleFileName("sample.docx");
       OPCPackage pkg = OPCPackage.open(filepath, PackageAccess.READ);
       try {
           int checked = 0;
           for (PackagePart part : pkg.getParts()) {
              // Can get the size of zip parts
              if (part.getPartName().getName().equals("/word/document.xml")) {
                 checked++;
                 assertEquals(ZipPackagePart.class, part.getClass());
                 assertEquals(6031l, part.getSize());
              }
              if (part.getPartName().getName().equals("/word/fontTable.xml")) {
                 checked++;
                 assertEquals(ZipPackagePart.class, part.getClass());
                 assertEquals(1312l, part.getSize());
              }
              
              // But not from the others
              if (part.getPartName().getName().equals("/docProps/core.xml")) {
                 checked++;
                 assertEquals(PackagePropertiesPart.class, part.getClass());
                 assertEquals(-1, part.getSize());
              }
           }
           // Ensure we actually found the parts we want to check
           assertEquals(3, checked);
       } finally {
           pkg.close();
       }
    }

    @Test
    public void replaceContentType() throws Exception {
        InputStream is = OpenXML4JTestDataSamples.openSampleStream("sample.xlsx");
        OPCPackage p = OPCPackage.open(is);

        ContentTypeManager mgr = getContentTypeManager(p);

        assertTrue(mgr.isContentTypeRegister("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"));
        assertFalse(mgr.isContentTypeRegister("application/vnd.ms-excel.sheet.macroEnabled.main+xml"));

        assertTrue(
                p.replaceContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml",
                "application/vnd.ms-excel.sheet.macroEnabled.main+xml")
        );

        assertFalse(mgr.isContentTypeRegister("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"));
        assertTrue(mgr.isContentTypeRegister("application/vnd.ms-excel.sheet.macroEnabled.main+xml"));
    }

    @Test(expected=IOException.class)
    public void zipBombCreateAndHandle() throws Exception {
        // #50090 / #56865
        ZipFile zipFile = ZipHelper.openZipFile(OpenXML4JTestDataSamples.getSampleFile("sample.xlsx"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream append = new ZipOutputStream(bos);
        // first, copy contents from existing war
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e2 = entries.nextElement();
            ZipEntry e = new ZipEntry(e2.getName());
            e.setTime(e2.getTime());
            e.setComment(e2.getComment());
            e.setSize(e2.getSize());
            
            append.putNextEntry(e);
            if (!e.isDirectory()) {
                InputStream is = zipFile.getInputStream(e);
                if (e.getName().equals("[Content_Types].xml")) {
                    ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
                    IOUtils.copy(is, bos2);
                    long size = bos2.size()-"</Types>".length();
                    append.write(bos2.toByteArray(), 0, (int)size);
                    byte spam[] = new byte[0x7FFF];
                    for (int i=0; i<spam.length; i++) spam[i] = ' ';
                    while (size < 0x7FFF0000) {
                        append.write(spam);
                        size += spam.length;
                    }
                    append.write("</Types>".getBytes());
                    size += 8;
                    e.setSize(size);
                } else {
                    IOUtils.copy(is, append);
                }
            }
            append.closeEntry();
        }
        
        append.close();
        zipFile.close();

        byte buf[] = bos.toByteArray();
        bos = null;
        
        Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(buf));
        wb.getSheetAt(0);
        wb.close();
    }
    
    @Test
    public void zipBombCheckSizes() throws Exception {
        File file = OpenXML4JTestDataSamples.getSampleFile("sample.xlsx");

        try {
            double min_ratio = Double.MAX_VALUE;
            long max_size = 0;
            ZipFile zf = ZipHelper.openZipFile(file);
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                double ratio = (double)ze.getCompressedSize() / (double)ze.getSize();
                min_ratio = Math.min(min_ratio, ratio);
                max_size = Math.max(max_size, ze.getSize());
            }
            zf.close();
    
            // use values close to, but within the limits 
            ZipSecureFile.setMinInflateRatio(min_ratio-0.002);
            ZipSecureFile.setMaxEntrySize(max_size+1);
            Workbook wb = WorkbookFactory.create(file);
            wb.close();
    
            // check ratio out of bounds
            ZipSecureFile.setMinInflateRatio(min_ratio+0.002);
            try {
                wb = WorkbookFactory.create(file);
                wb.close();
                // this is a bit strange, as there will be different exceptions thrown
                // depending if this executed via "ant test" or within eclipse
                // maybe a difference in JDK ...
            } catch (InvalidFormatException e) {
                checkForZipBombException(e);
            } catch (POIXMLException e) {
                checkForZipBombException(e);
            }
    
            // check max entry size ouf of bounds
            ZipSecureFile.setMinInflateRatio(min_ratio-0.002);
            ZipSecureFile.setMaxEntrySize(max_size-1);
            try {
                wb = WorkbookFactory.create(file, null, true);
                wb.close();
            } catch (InvalidFormatException e) {
                checkForZipBombException(e);
            } catch (POIXMLException e) {
                checkForZipBombException(e);
            }
        } finally {
            // reset otherwise a lot of ooxml tests will fail
            ZipSecureFile.setMinInflateRatio(0.01d);
            ZipSecureFile.setMaxEntrySize(0xFFFFFFFFl);            
        }
    }

    private void checkForZipBombException(Throwable e) {
        if(e instanceof InvocationTargetException) {
            InvocationTargetException t = (InvocationTargetException)e;
            IOException t2 = (IOException)t.getTargetException();
            if("Zip bomb detected! Exiting.".equals(t2.getMessage())) {
                return;
            }
        }
        
        if ("Zip bomb detected! Exiting.".equals(e.getMessage())) {
            return;
        }
        
        // recursively check the causes for the message as it can be nested further down in the exception-tree
        if(e.getCause() != null && e.getCause() != e) {
            checkForZipBombException(e.getCause());
            return;
        }

        throw new IllegalStateException("Expected to catch an Exception because of a detected Zip Bomb, but did not find the related error message in the exception", e);        
    }
}
