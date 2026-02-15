package com.app.catalog.domain;

import com.app.catalog.ProductService;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.payloads.ProductResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/social")
public class SocialCommerceController {

    private final ProductService productService;

    public SocialCommerceController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping(value = "/meta-catalog.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getMetaCatalog() {
        // Fetch all products (limit to 100 for now or handle pagination)
        ProductResponse response = productService.getAllProducts(0, 100, "productId", "asc");
        List<ProductDTO> products = response.content();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<rss xmlns:g=\"http://base.google.com/ns/1.0\" version=\"2.0\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>Vaabhi Storefront Catalog</title>\n");
        xml.append("    <link>https://vaabhi.com</link>\n");
        xml.append("    <description>Product feed for Instagram and WhatsApp Shopping</description>\n");

        for (ProductDTO p : products) {
            xml.append("    <item>\n");
            xml.append("      <g:id>").append(p.productId()).append("</g:id>\n");
            xml.append("      <g:title><![CDATA[").append(p.productName()).append("]]></g:title>\n");
            xml.append("      <g:description><![CDATA[").append(p.description()).append("]]></g:description>\n");
            xml.append("      <g:link>https://vaabhi.com/products/").append(p.productId()).append("</g:link>\n");
            xml.append("      <g:image_link>https://vaabhi.com/api/v1/public/products/image/").append(p.image()).append("</g:image_link>\n");
            xml.append("      <g:availability>").append(p.quantity() > 0 ? "in stock" : "out of stock").append("</g:availability>\n");
            xml.append("      <g:price>").append(p.specialPrice() != null ? p.specialPrice() : p.price()).append(" INR</g:price>\n");
            xml.append("      <g:brand>Vaabhi</g:brand>\n");
            xml.append("      <g:condition>new</g:condition>\n");
            xml.append("      <g:google_product_category>Apparel &amp; Accessories</g:google_product_category>\n");
            xml.append("    </item>\n");
        }

        xml.append("  </channel>\n");
        xml.append("</rss>");

        return xml.toString();
    }
}
