package com.shivhub.backend.service;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.repository.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Component
public class ProductExtensionSeeder {
    private final SubCategoryRepository subcategories; private final CategorySpecificationTemplateRepository templates; private final DeliveryRuleService delivery;
    public ProductExtensionSeeder(SubCategoryRepository subcategories,CategorySpecificationTemplateRepository templates,DeliveryRuleService delivery){this.subcategories=subcategories;this.templates=templates;this.delivery=delivery;}
    @EventListener(ApplicationReadyEvent.class) @Transactional
    public void initialize(){
        delivery.initialize();
        for(SubCategory sub:subcategories.findAll()) {
            if(!sub.isActive()||!sub.getCategory().isActive()||"Mobiles".equalsIgnoreCase(sub.getCategory().getName()))continue;
            // Deactivated Admin fields are still configuration; never recreate them on restart.
            if(templates.existsBySubCategoryId(sub.getId()) || !templates.findActiveForCategory(sub.getCategory().getId(),sub.getId()).isEmpty())continue;
            String name=sub.getName().toLowerCase(Locale.ROOT);
            List<String> fields;
            if(List.of("shirts","t-shirts","boys clothing","girls clothing","tops","kurtis","dresses","jeans","trousers").contains(name))fields=List.of("size|Size|S,M,L,XL,XXL|variant", "color|Color||variant", "fabric|Fabric", "fit|Fit|Regular,Slim,Relaxed", "sleeve|Sleeve type", "pattern|Pattern", "collar|Collar type", "gender|Gender", "occasion|Occasion", "wash_care|Wash care", "size_chart|Size chart");
            else if(List.of("shoes","footwear","kids footwear").contains(name))fields=List.of("size|Size||variant","color|Color||variant","gender|Gender","material|Material","sole_material|Sole material","closure|Closure type");
            else if(name.equals("laptops"))fields=List.of("processor|Processor","ram|RAM||variant","storage|Storage||variant","color|Color||variant","screen_size|Screen size","operating_system|Operating system","graphics|Graphics","ports|Ports","warranty|Warranty");
            else if(name.equals("led tvs")||name.equals("smart tvs"))fields=List.of("screen_size|Screen size||variant","display_type|Display type|LED,QLED,OLED","resolution|Resolution","smart_tv|Smart TV|Yes,No","refresh_rate|Refresh rate","sound_output|Sound output","warranty|Warranty");
            else if(List.of("headphones","earbuds","bluetooth speakers").contains(name))fields=List.of("type|Type","connectivity|Connectivity|Wired,Wireless,Bluetooth","battery_life|Battery life","color|Color||variant","compatibility|Compatible devices","warranty|Warranty");
            else if(sub.getCategory().getName().equalsIgnoreCase("Grocery & Daily Needs"))fields=List.of("quantity|Weight / volume","unit|Unit|g,kg,ml,l,piece","flavour|Flavour / variant||variant","pack_size|Pack size","expiry_date|Expiry date");
            else if(sub.getCategory().getName().equalsIgnoreCase("Beauty & Personal Care"))fields=List.of("skin_hair_type|Skin / hair type","shade|Shade||variant","quantity|Quantity","expiry_date|Expiry date","ingredients|Ingredients");
            else if(sub.getCategory().getName().equalsIgnoreCase("Home & Essentials"))fields=List.of("material|Material","dimensions|Dimensions","color|Color||variant","capacity|Capacity","pack_count|Pack count","warranty|Warranty");
            else if(sub.getCategory().getName().equalsIgnoreCase("Automobile"))fields=List.of("vehicle_compatibility|Vehicle compatibility","part_number|Part number","model_year|Model / year compatibility","warranty|Warranty");
            else continue;
            int order=0;for(String raw:fields){String[] parts=raw.split("\\|",-1);CategorySpecificationTemplate f=new CategorySpecificationTemplate();f.setCategory(sub.getCategory());f.setSubCategory(sub);f.setSpecificationKey(parts[0]);f.setDisplayLabel(parts[1]);f.setInputType(parts.length>2&&!parts[2].isBlank()?"SELECT":"TEXT");f.setOptionsJson(parts.length>2&&!parts[2].isBlank()?parts[2]:null);f.setVariantEnabled(parts.length>3&&parts[3].equals("variant"));f.setFilterable(f.isVariantEnabled());f.setDisplayOrder(order++);f.setActive(true);templates.save(f);}
        }
    }
}
