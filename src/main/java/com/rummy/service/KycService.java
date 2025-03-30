package com.rummy.service;

import com.rummy.model.KycDocument;
import com.rummy.model.KycStatus;
import com.rummy.model.User;
import com.rummy.repository.KycDocumentRepository;
import com.rummy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class KycService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    private final Path documentStoragePath = Paths.get("uploads/kyc");

    public KycService() {
        try {
            Files.createDirectories(documentStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create document storage directory", e);
        }
    }

    public KycDocument uploadDocument(Long userId, String documentType, String documentNumber, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Validate document type
        if (!isValidDocumentType(documentType)) {
            throw new IllegalArgumentException("Invalid document type");
        }

        // Check if document already exists
        List<KycDocument> existingDocs = kycDocumentRepository.findByUserAndDocumentType(user, documentType);
        if (!existingDocs.isEmpty()) {
            throw new IllegalArgumentException("Document type already uploaded");
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        try {
            Files.copy(file.getInputStream(), documentStoragePath.resolve(fileName));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store document", e);
        }

        KycDocument document = new KycDocument();
        document.setUser(user);
        document.setDocumentType(documentType);
        document.setDocumentNumber(documentNumber);
        document.setDocumentPath(fileName);
        document.setVerificationStatus(KycStatus.PENDING);

        user.setKycStatus(KycStatus.IN_PROGRESS);
        userRepository.save(user);

        return kycDocumentRepository.save(document);
    }

    public KycDocument verifyDocument(Long documentId, KycStatus status, String remarks) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        document.setVerificationStatus(status);
        document.setVerificationRemarks(remarks);

        // Update user's KYC status based on all documents
        User user = document.getUser();
        List<KycDocument> allDocuments = kycDocumentRepository.findByUser(user);
        updateUserKycStatus(user, allDocuments);

        return kycDocumentRepository.save(document);
    }

    public List<KycDocument> getUserDocuments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return kycDocumentRepository.findByUser(user);
    }

    private boolean isValidDocumentType(String documentType) {
        return documentType != null && (
            documentType.equals("AADHAR") ||
            documentType.equals("PAN") ||
            documentType.equals("PASSPORT")
        );
    }

    private void updateUserKycStatus(Long userId, KycStatus status) {
        if (status.isEmpty()) {
            userId.setKycStatus(KycStatus.PENDING);
        } else if (status.stream().allMatch(doc -> doc.getVerificationStatus() == KycStatus.APPROVED)) {
            userId.setKycStatus(KycStatus.APPROVED);
        } else if (status.stream().anyMatch(doc -> doc.getVerificationStatus() == KycStatus.REJECTED)) {
            userId.setKycStatus(KycStatus.REJECTED);
        } else {
            userId.setKycStatus(KycStatus.IN_PROGRESS);
        }
        userRepository.save(userId);
    }
}