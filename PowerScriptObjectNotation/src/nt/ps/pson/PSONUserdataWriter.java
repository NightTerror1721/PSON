/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nt.ps.pson;

import java.io.IOException;
import nt.ps.pson.PSONWriter.WriterOperations;

/**
 *
 * @author Asus
 */
@FunctionalInterface
public interface PSONUserdataWriter
{
    void writePSONProperties(WriterOperations wops) throws IOException;
}
