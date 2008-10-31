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

package org.apache.poi.hssf.record;

import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndian;

/**
 * The number format index record indexes format table.  This applies to an axis.<p/>
 * 
 * @author Glen Stampoultzis (glens at apache.org)
 */
public final class NumberFormatIndexRecord extends Record {
    public final static short      sid                             = 0x104E;
    private  short      field_1_formatIndex;


    public NumberFormatIndexRecord()
    {

    }

    public NumberFormatIndexRecord(RecordInputStream in)
    {
        field_1_formatIndex            = in.readShort();
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[IFMT]\n");
        buffer.append("    .formatIndex          = ")
            .append("0x").append(HexDump.toHex(  getFormatIndex ()))
            .append(" (").append( getFormatIndex() ).append(" )");
        buffer.append(System.getProperty("line.separator")); 

        buffer.append("[/IFMT]\n");
        return buffer.toString();
    }

    public int serialize(int offset, byte[] data)
    {
        int pos = 0;

        LittleEndian.putShort(data, 0 + offset, sid);
        LittleEndian.putShort(data, 2 + offset, (short)(getRecordSize() - 4));

        LittleEndian.putShort(data, 4 + offset + pos, field_1_formatIndex);

        return getRecordSize();
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    public Object clone() {
        NumberFormatIndexRecord rec = new NumberFormatIndexRecord();
    
        rec.field_1_formatIndex = field_1_formatIndex;
        return rec;
    }




    /**
     * Get the format index field for the NumberFormatIndex record.
     */
    public short getFormatIndex()
    {
        return field_1_formatIndex;
    }

    /**
     * Set the format index field for the NumberFormatIndex record.
     */
    public void setFormatIndex(short field_1_formatIndex)
    {
        this.field_1_formatIndex = field_1_formatIndex;
    }
}
