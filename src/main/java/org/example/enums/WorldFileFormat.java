package org.example.enums;

public enum WorldFileFormat {
    DB(".db"),
    DB_OLD(".db.old"),
    FWL(".fwl"),
    FWL_OLD(".fwl.old");



    private final String formatName;
    WorldFileFormat(){
        this.formatName = name().toLowerCase();
    }
    WorldFileFormat(String formatName){
        this.formatName = formatName;
    }
    public String getFormatName(){
        return formatName;
    }
}
