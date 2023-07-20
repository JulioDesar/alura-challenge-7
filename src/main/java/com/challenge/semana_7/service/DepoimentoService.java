package com.challenge.semana_7.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.challenge.semana_7.model.Depoimentos;
import com.challenge.semana_7.repository.DepoimentoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;


@Service
@Slf4j
public class DepoimentoService {

    @Value("${application.bucket.name}")
    private String bucketName;

    @Autowired
    private AmazonS3 s3Client;

    @Autowired
    private DepoimentoRepository depoimentoRepository;

    public Iterable<Depoimentos> getAll() {
        Iterable<Depoimentos> depoimentos = depoimentoRepository.findAll();
        depoimentos.forEach(depoimento -> {
            depoimento.setFotoPath(getS3ImageUrl(depoimento.getFotoPath()));
        });
        return depoimentos;
    }

    public Depoimentos getById(Long id) {
        Depoimentos depoimento = depoimentoRepository.findById(id).get();
        depoimento.setFotoPath(getS3ImageUrl(depoimento.getFotoPath()));
        return depoimento;
    }

    public Depoimentos save(Depoimentos depoimentos, MultipartFile fotos) {
        try {
            String fileName = System.currentTimeMillis() + "_" + fotos.getOriginalFilename();
            String filePath = uploadFileToS3(fileName, fotos);

            depoimentos.setFotoPath(filePath);
            return depoimentoRepository.save(depoimentos);
        } catch (Exception e) {
            log.error("Erro ao salvar depoimento", e);
            return null;
        }
    }

    public byte[] downloadFile(String keyName) {
        S3Object s3Object = s3Client.getObject(bucketName, keyName);
        S3ObjectInputStream inputStream = s3Object.getObjectContent();
        try {
            return IOUtils.toByteArray(inputStream);
        } catch (Exception e) {
            log.error("Erro ao baixar arquivo", e);
        }
        return null;
    }

    private File convertMultiPartFileToFile(MultipartFile file) {
        File convertedFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        } catch (IOException e) {
            log.error("Error converting multipartFile to file", e);
        }
        return convertedFile;
    }

    private String uploadFileToS3(String fileName, MultipartFile file) {
        try {
            File fileObj = convertMultiPartFileToFile(file);
            s3Client.putObject(new PutObjectRequest(bucketName, fileName, fileObj));
            fileObj.delete();
            return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
        } catch (Exception e) {
            log.error("Erro ao enviar o arquivo para o s3", e);
            return null;
        }
    }

    private String getS3ImageUrl(String fotoPath) {
        Date expiration = new Date(System.currentTimeMillis() + 3600000); // Define a validade do link por 1 hora
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, fotoPath)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);
        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }
}
