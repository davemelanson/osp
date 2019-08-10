/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package csm.ch02;
// gets needed classes, asterisk * means get all classes in controls subdirectory

import org.opensourcephysics.controls.*;

/**
 * CalculationApp demonstrates the use of a CalculationControl.
 *
 * @author Wolfgang Christian, Jan Tobochnik, Harvey Gould
 * @version 1.0  05/07/05
 */
public class CalculationApp extends AbstractCalculation {

  /**
   * Does a calculation.
   */
  public void calculate() { // Does a calculation
    control.println("Calculation button pressed.");
    double x = control.getDouble("x value"); // String must match argument of setValue
    control.println("x*x = "+(x*x));
    control.println("random = "+Math.random());
  }

  /**
   * Resets the program to its initial state.
   */
  public void reset() {
	control.setValue("a value", 1.0); // describes parameter and sets its value
    control.setValue("x value", 10.0); // describes parameter and sets its value
  }
  
  public void myFunction() {
	  System.out.println("Button pressed.");
  }

  /**
   * Starts the Java application.
   * @param args  command line parameters
   */
  public static void main(String[] args) { // Create a calculation control structure using this class
	  CalculationControl control=CalculationControl.createApp(new CalculationApp());
	  control.addButton("System.out.println", "Press Me");
	  control.addButton("myFunction", "Press Me");
  }
}

/* 
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
