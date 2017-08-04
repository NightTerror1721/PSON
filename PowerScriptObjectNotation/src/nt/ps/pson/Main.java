/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nt.ps.pson;

import java.io.File;
import java.io.IOException;
import nt.ps.lang.PSObject;
import nt.ps.lang.PSValue;
import nt.ps.lang.core.PSObjectReference;

/**
 *
 * @author Asus
 */
public final class Main
{
    public static void main(String[] args) throws IOException, PSONException
    {
        PSObject obj = PSON.read(new File("test.pson"));
        System.out.println(PSObjectReference.toString(obj, true));
        
        obj.setProperty("marc_test", PSValue.valueOf(true, new int[]{ 5, 10, 65, 145 }));
        PSON.write(obj, false, new File("test2.pson"));
        System.out.println(PSObjectReference.toString(PSON.read(new File("test2.pson")), true));
    }
}
