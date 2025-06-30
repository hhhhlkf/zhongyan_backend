package com.gosling.bms.service;

public interface TransferService {

    Boolean isTransferAvailable();

    Boolean startTransfer();

    Boolean stopTransfer();
}
