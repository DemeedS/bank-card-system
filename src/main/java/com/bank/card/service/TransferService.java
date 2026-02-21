package com.bank.card.service;

import com.bank.card.dto.request.TransferRequest;
import com.bank.card.dto.response.TransferResponse;
import com.bank.card.entity.User;

public interface TransferService {

    TransferResponse transfer(TransferRequest request, User currentUser);
}
