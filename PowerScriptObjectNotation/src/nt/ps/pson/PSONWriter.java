/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nt.ps.pson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import nt.ps.exception.PSRuntimeException;
import nt.ps.lang.PSObject;
import nt.ps.lang.PSObject.PropertyEntry;
import nt.ps.lang.PSValue;

/**
 *
 * @author Asus
 */
public final class PSONWriter implements AutoCloseable
{
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private final BufferedWriter output;
    
    public PSONWriter(OutputStream output, int bufferLen)
    {
        if(bufferLen < 1)
            throw new IllegalArgumentException("bufferLen cannot be less than 1");
        this.output = new BufferedWriter(new OutputStreamWriter(output));
    }
    
    public PSONWriter(Writer writer, int bufferLen)
    {
        if(bufferLen < 1)
            throw new IllegalArgumentException("bufferLen cannot be less than 1");
        output = new BufferedWriter(Objects.requireNonNull(writer));
    }
    
    private void writeValue(String identation, PSValue value) throws IOException
    {
        switch(value.getPSType())
        {
            case UNDEFINED:
            case NULL:
            case NUMBER:
            case BOOLEAN:
                output.write(value.toJavaString());
                break;
            case STRING: output.write("\"" + value.toJavaString() + "\""); break;
            case TUPLE:
            case ARRAY:
                writeArray(identation, value);
                break;
            case MAP: writeMap(identation, value); break;
            case OBJECT: writeObject(identation, value.toPSObject(), true); break;
            default:
                throw new PSRuntimeException("Invalid PS type: " + value.getPSType());
        }
    }
    
    private void writeArray(String identation, PSValue array) throws IOException
    {
        output.write("[");
        List<PSValue> list = array.toJavaList();
        int len = list.size(), count = 0;
        for(PSValue value : array.toJavaList())
        {
            writeValue(identation, value);
            if(++count < len)
                output.write(", ");
        }
        output.write("]");
    }
    
    private void writeMap(String identation, PSValue mapObj) throws IOException
    {
        String newIdentation = identation + "    ";
        Map<PSValue, PSValue> map = mapObj.toJavaMap();
        int len = map.size(), count = 0;
        for(Map.Entry<PSValue, PSValue> e : map.entrySet())
        {
            output.write(newIdentation + e.getKey().toJavaString() + ": ");
            writeValue(newIdentation, e.getValue());
            output.append(++count < len ? ",\n" : "\n");
        }
        output.write(identation + "}");
    }
    
    private static String propertyName(String name)
    {
        return ID_PATTERN.matcher(name).matches()
                ? replaceScapeChars(name)
                : "\"" + replaceScapeChars(name) + "\"";
    }
    
    private static String replaceScapeChars(String name)
    {
        return name.replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n")
                .replace("\u0000", "\\0").replace("\\", "\\\\").replace("\"", "\\\"").replace("\'", "\\\'");
    }
    
    private void writeObject(String identation, PSObject object, boolean wrapped) throws IOException
    {
        String newIdentation;
        if(wrapped)
        {
            newIdentation = identation + "    ";
            output.write("{\n");
        }
        else newIdentation = identation;
        int len = object.getPropertyCount(), count = 0;
        for(PropertyEntry p : object.properties())
        {
            output.write(newIdentation + propertyName(p.getName()) + ": ");
            writeValue(newIdentation, p.getValue());
            output.append(++count < len ? ",\n" : "\n");
        }
        if(wrapped)
            output.write(identation + "}");
    }
    
    public final void writeObject(PSObject object, boolean wrapped) throws IOException { writeObject("", object, wrapped); }
    
    private void writeUserdata(String identation, PSONUserdataWriter object, boolean wrapped) throws IOException
    {
        String newIdentation;
        if(wrapped)
        {
            newIdentation = identation + "    ";
            output.write("{\n");
        }
        else newIdentation = identation;
        WriterOperations wops = new WriterOperations(newIdentation);
        object.writePSONProperties(wops);
        if(wrapped)
            output.write(identation + "}");
    }
    
    private void writeUserdataArray(String identation, PSONUserdataWriter[] objects) throws IOException
    {
        output.write("[");
        int count = 0;
        for(PSONUserdataWriter value : objects)
        {
            writeUserdata(identation, value, true);
            if(++count < objects.length)
                output.write(", ");
        }
        output.write("]");
    }
    
    private void writeUserdataMap(String identation, Map<String, PSONUserdataWriter> map, boolean wrapped) throws IOException
    {
        String newIdentation;
        if(wrapped)
        {
            newIdentation = identation + "    ";
            output.write("{\n");
        }
        else newIdentation = identation;
        int len = map.size(), count = 0;
        for(Map.Entry<String, PSONUserdataWriter> e : map.entrySet())
        {
            output.write(newIdentation + propertyName(e.getKey()) + ": ");
            writeUserdata(newIdentation, e.getValue(), true);
            output.append(++count < len ? ",\n" : "\n");
        }
        if(wrapped)
            output.write(identation + "}");
    }
    
    public final void writeUserdata(PSONUserdataWriter object, boolean wrapped) throws IOException { writeUserdata("", object, wrapped); }
    
    public final void flush() throws IOException
    {
        output.flush();
    }

    @Override
    public final void close() throws IOException
    {
        output.close();
    }
    
    public final class WriterOperations
    {
        private int count = 0;
        private final String identation;
        
        private WriterOperations(String identation) { this.identation = identation; }
        
        public final void write(String name, PSValue value) throws IOException
        {
            if(count++ > 0)
                output.write(",\n" + identation);
            output.write(propertyName(name) + ": ");
            writeValue(identation, value);
        }
        
        public final void write(String name, byte value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, short value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, int value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, long value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, float value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, double value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, boolean value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, char value) throws IOException { write(name, PSValue.valueOf(value)); }
        
        public final void write(String name, Byte value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Short value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Integer value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Long value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Float value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Double value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Boolean value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Character value) throws IOException { write(name, PSValue.valueOf(value)); }
        
        public final void write(String name, String value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Enum<?> value) throws IOException { write(name, PSValue.valueOf(value.ordinal())); }
        public final void write(String name, List<? extends PSValue> value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Map<PSValue, PSValue> value) throws IOException { write(name, PSValue.valueOf(value)); }
        
        public final void write(String name, byte... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, short... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, int... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, long... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, float... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, double... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, boolean... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, char... value) throws IOException { write(name, PSValue.valueOf(value)); }
        
        public final void write(String name, Byte... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Short... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Integer... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Long... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Float... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Double... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Boolean... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, Character... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, String... value) throws IOException { write(name, PSValue.valueOf(value)); }
        public final void write(String name, PSValue... value) throws IOException { write(name, PSValue.valueOf(value)); }
        
        public final <T> void write(String name, List<T> value, Function<? super T, PSValue> caster) throws IOException { write(name, PSValue.valueOf(value, caster)); }
        public final <K, V> void write(String name, Map<K, V> value, Function<? super K, PSValue> keyCaster, Function<? super V, PSValue> valueCaster) throws IOException
        {
            write(name, PSValue.valueOf(value, keyCaster, valueCaster));
        }
        
        public final void write(String name, PSONUserdataWriter userdata) throws IOException
        {
            if(count++ > 0)
                output.write(",\n" + identation);
            output.write(propertyName(name) + ": ");
            writeUserdata(identation, userdata, true);
        }
        
        public final void write(String name, PSONUserdataWriter... userdatas) throws IOException
        {
            if(count++ > 0)
                output.write(",\n" + identation);
            output.write(propertyName(name) + ": ");
            writeUserdataArray(identation, userdatas);
        }
        
        public final void writeUserdataMap(String name, Map<String, PSONUserdataWriter> userdataMap) throws IOException
        {
            if(count++ > 0)
                output.write(",\n" + identation);
            output.write(propertyName(name) + ": ");
            PSONWriter.this.writeUserdataMap(identation, userdataMap, true);
        }
    }
}
