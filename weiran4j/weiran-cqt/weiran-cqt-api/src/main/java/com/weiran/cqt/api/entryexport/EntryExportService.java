package com.weiran.cqt.api.entryexport;

public interface EntryExportService {
    byte[] export(EntryExportCommand command);
}
