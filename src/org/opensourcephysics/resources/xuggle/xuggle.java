/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.resources.xuggle;
import java.io.IOException;
import java.io.InputStream;
import java.util.PropertyResourceBundle;

/**
 * Resource loader for OSP tools.xuggle package.  Resources are obtained from properties.
 * 
 * Defining a Java resource class speeds up resource loading, particularly for applets because
 * a connection to the server is not required. 
 * 
 * @author Wolfgang Christian
*/
public class xuggle extends PropertyResourceBundle {
  // relative path to strings
  static String res = "xuggle.properties"; //$NON-NLS-1$

  /**
   * Constructor tools
   * @throws IOException
   */
  public xuggle() throws IOException {
    this(xuggle.class.getResourceAsStream(res));
  }

  /**
   * Constructor tools
   * @param stream
   * @throws IOException
   */
  public xuggle(InputStream stream) throws IOException {
    super(stream);
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
 * Copyright (c) 2017  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
