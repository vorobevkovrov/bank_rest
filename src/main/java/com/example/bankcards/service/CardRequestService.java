package com.example.bankcards.service;

import com.example.bankcards.dto.request.BlockCardRequest;
import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.util.CardMapper;

import java.util.List;

public interface CardRequestService {
    CardRequestResponse requestCardBlock(BlockCardRequest request, String username);
    CardRequestResponse approveBlockRequest(Long requestId, String adminUsername);
    CardRequestResponse rejectBlockRequest(Long requestId, String adminUsername);
    List<CardRequestResponse> getAllPendingRequest(String adminUsername);
}
