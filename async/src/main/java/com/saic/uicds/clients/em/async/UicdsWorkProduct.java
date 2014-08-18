/**
 * 
 */
package com.saic.uicds.clients.em.async;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.PropertiesType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.util.Common;

/**
 * UICDS Work Product
 * 
 * This class represents a UICDS Work Product and provides accessor methods for the type, id,
 * content, and WorkProductIdentification and WorkProductProperties elements.
 * 
 * @author roger
 * 
 */
public class UicdsWorkProduct {

    private IdentificationType workProductIdentifier;
    private WorkProduct workProduct;
    private String type;
    private String workProductID;

    private static final String PRECISS_NS = "http://www.saic.com/precis/2009/06/structures";
    @SuppressWarnings("unused")
    private static final String PRECISB_NS = "http://www.saic.com/precis/2009/06/base";
    private static final String WORKPRODUCT_IDENTIFICATION = "WorkProductIdentification";
    private static final String WORKPRODUCT_PROPERTIES = "WorkProductProperties";

    public static final String WEB_MAP_CONTEXT_NS = "http://www.opengis.net/context";
    public static final String MAP_WP_TYPE = "MapViewContext";

    public UicdsWorkProduct(IdentificationType id) {
        setIdentificationInformation(id);
    }

    public UicdsWorkProduct(WorkProduct wp) {
        workProduct = wp;
        setIdentificationInformation(getIdentificationElement(workProduct));
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("WorkProductIdentifier:\n");
        sb.append(workProductIdentifier);
        sb.append("\n");
        sb.append("WorkProduct:\n");
        sb.append(workProduct);
        sb.append("\n");
        return sb.toString();
    }

    private void setIdentificationInformation(IdentificationType id) {
        if (id != null) {
            workProductIdentifier = id;
            type = id.getType().getStringValue();
            workProductID = id.getIdentifier().getStringValue();
        }
    }

    /**
     * Get the work product id
     * 
     * @return String
     */
    public IdentificationType getWorkProductIdentifier() {
        return workProductIdentifier;
    }

    /**
     * Get the work product type
     * 
     * @return String
     */
    public String getType() {
        return type;
    }

    /**
     * Get the work product id
     * 
     * @return String
     */
    public String getId() {
        return workProductID;
    }

    /**
     * Get the WorkProduct object
     * 
     * @return
     */
    public WorkProduct getWorkProduct() {
        return workProduct;
    }

    /**
     * Get the XmlObject version of the work product content
     * 
     * @return StructuredPayload
     */
    public XmlObject getContent() {
        if (workProduct == null)
            return null;

        if (workProduct.sizeOfStructuredPayloadArray() > 0) {
            return workProduct.getStructuredPayloadArray(0);
        } else {
            return null;
        }
    }

    public XmlObject getContent(String namespace, String elementName) {
        XmlObject[] objects = workProduct.getStructuredPayloadArray(0).selectChildren(
            new QName(namespace, elementName));
        if (objects.length > 0) {
            return objects[0];
        }
        return null;
    }

    /**
     * Set the Identifier element in the given abstract package metadata
     * 
     * @param packageMetadataExtensionAbstract
     * @param workProductIdentification
     */
    public static final void setIdentifierElement(XmlObject packageMetadataExtensionAbstract,
        XmlObject workProductIdentification) {
        Common.substitute(packageMetadataExtensionAbstract, PRECISS_NS, WORKPRODUCT_IDENTIFICATION,
            IdentificationType.type, workProductIdentification);
    }

    /**
     * Set the Properties element in the given abstract package metadata
     * 
     * @param packageMetadataExtensionAbstract
     * @param workProductProperties
     */
    public static final void setPropertiesElement(XmlObject packageMetadataExtensionAbstract,
        XmlObject workProductProperties) {
        Common.substitute(packageMetadataExtensionAbstract, PRECISS_NS, WORKPRODUCT_PROPERTIES,
            PropertiesType.type, workProductProperties);
    }

    /**
     * Get the Identification of the given WorkProduct
     * 
     * @param workProduct
     * @return
     */
    public static final IdentificationType getIdentificationElement(WorkProduct workProduct) {
        IdentificationType id = null;
        if (workProduct == null) {
            System.err.println("Trying to get an identification element from a null work product");
        }
        if (workProduct != null && workProduct.getPackageMetadata() != null) {
            XmlObject[] objects = workProduct.getPackageMetadata().selectChildren(
                new QName(PRECISS_NS, WORKPRODUCT_IDENTIFICATION));
            if (objects.length > 0) {
                id = (IdentificationType) objects[0];
            }
        }
        return id;
    }

    /**
     * Get the Properties element from the given WorkProduct
     * 
     * @param workProduct
     * @return
     */
    public static final PropertiesType getPropertiesElement(WorkProduct workProduct) {
        PropertiesType properties = null;
        if (workProduct != null && workProduct.getPackageMetadata() != null) {
            XmlObject[] objects = workProduct.getPackageMetadata().selectChildren(
                new QName(PRECISS_NS, WORKPRODUCT_PROPERTIES));
            if (objects.length > 0) {
                properties = (PropertiesType) objects[0];
            }
        }
        return properties;
    }

}
