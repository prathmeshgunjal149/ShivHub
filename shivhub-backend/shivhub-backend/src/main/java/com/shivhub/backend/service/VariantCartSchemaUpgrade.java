package com.shivhub.backend.service;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.util.*;

/** Hibernate update cannot retire the old two-column cart uniqueness. No row/table/column is removed. */
@Component @Order(-100)
public class VariantCartSchemaUpgrade implements ApplicationRunner {
    private final DataSource source;private final JdbcTemplate jdbc;
    public VariantCartSchemaUpgrade(DataSource source,JdbcTemplate jdbc){this.source=source;this.jdbc=jdbc;}
    public void run(ApplicationArguments args)throws Exception{
        try(var connection=source.getConnection()){
            if(!connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL"))return;
            Set<String> oldIndexes=new HashSet<>();Map<String,Set<String>> columns=new HashMap<>();
            try(var indexes=connection.getMetaData().getIndexInfo(connection.getCatalog(),null,"cart_items",true,false)){
                while(indexes.next()){String name=indexes.getString("INDEX_NAME"),column=indexes.getString("COLUMN_NAME");if(name!=null&&column!=null)columns.computeIfAbsent(name,k->new HashSet<>()).add(column.toLowerCase(Locale.ROOT));}
            }
            columns.forEach((name,values)->{if(values.equals(Set.of("customer_id","product_id")))oldIndexes.add(name);});
            if(oldIndexes.isEmpty())return;
            jdbc.update("UPDATE cart_items SET selection_key = COALESCE(product_variant_id, 0) WHERE selection_key <> COALESCE(product_variant_id, 0)");
            if(columns.values().stream().noneMatch(values->values.equals(Set.of("customer_id","product_id","selection_key"))))jdbc.execute("ALTER TABLE cart_items ADD UNIQUE KEY uk_cart_customer_selection (customer_id, product_id, selection_key)");
            for(String index:oldIndexes){if(!index.matches("[A-Za-z0-9_]+"))throw new IllegalStateException("Unexpected cart index name; review migration manually");jdbc.execute("ALTER TABLE cart_items DROP INDEX `"+index+"`");}
        }
    }
}
