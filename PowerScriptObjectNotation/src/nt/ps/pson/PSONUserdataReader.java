/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nt.ps.pson;

import java.io.IOException;
import nt.ps.lang.PSValue;

/**
 *
 * @author Asus
 */
@FunctionalInterface
public interface PSONUserdataReader
{
    void readPSONProperty(String name, PSValue value) throws IOException;
}
