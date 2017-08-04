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
            output.write(newIdentation + "\"" + e.getKey().toJavaString() + "\": ");
            writeValue(newIdentation, e.getValue());
            output.append(++count < len ? ",\n" : "\n");
        }
        output.write(identation + "}");
    }
    
    private static String propertyName(String name)
    {
        return ID_PATTERN.matcher(name).matches()
                ? name
                : "\"" + name + "\"";
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
    
    public final void flush() throws IOException
    {
        output.flush();
    }

    @Override
    public final void close() throws IOException
    {
        output.close();
    }
}
