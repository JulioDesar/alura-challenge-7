package com.challenge.semana_7.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.challenge.semana_7.dto.GetDepoimentosDto;
import com.challenge.semana_7.dto.UploadDepoimentosDto;
import com.challenge.semana_7.model.Depoimentos;
import com.challenge.semana_7.repository.DepoimentoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class DepoimentoService {

    @Value("${application.bucket.name}")
    private String bucketName;

    @Autowired
    private AmazonS3 s3Client;

    @Autowired
    private DepoimentoRepository depoimentoRepository;

    public Iterable<GetDepoimentosDto> getAll() {
        Iterable<Depoimentos> depoimentos = depoimentoRepository.findAll();
        Iterable<GetDepoimentosDto> depoimentosDtoList = StreamSupport.stream(depoimentos.spliterator(), false)
                .map(depoimento -> new GetDepoimentosDto(depoimento.getId(), depoimento.getNome(), depoimento.getDepoimento(), depoimento.getFotoPath()))
                .collect(Collectors.toList());
        return depoimentosDtoList;
    }

    public GetDepoimentosDto getById(Long id) {
        Depoimentos depoimentos = depoimentoRepository.findById(id).get();
        return new GetDepoimentosDto(depoimentos.getId(), depoimentos.getNome(), depoimentos.getDepoimento(), depoimentos.getFotoPath());
    }

    public UploadDepoimentosDto save(UploadDepoimentosDto dp, MultipartFile fotos) {
        try {
            String fileName = System.currentTimeMillis() + "_" + fotos.getOriginalFilename();
            String filePath = uploadFileToS3(fileName, fotos);

            Depoimentos depoimentoSalvo = depoimentoRepository.save(new Depoimentos(null, dp.nome(), dp.depoimento(), filePath));
            UploadDepoimentosDto dpDto = new UploadDepoimentosDto(depoimentoSalvo.getNome(), depoimentoSalvo.getDepoimento(), depoimentoSalvo.getFotoPath());

            return dpDto;
        } catch (Exception e) {
            log.error("Erro ao salvar depoimento", e);
            return null;
        }
    }

    public UploadDepoimentosDto update(Long id, UploadDepoimentosDto dp, MultipartFile fotos) {
        Optional<Depoimentos> depoimentoOptional = depoimentoRepository.findById(id);
        if (depoimentoOptional.isEmpty()) {
            return null;
        }
        Depoimentos depoimento = depoimentoOptional.get();
        depoimento.setDepoimento(dp.depoimento());
        depoimento.setNome(dp.nome());
        if (fotos != null) {
            String fileName = System.currentTimeMillis() + "_" + fotos.getOriginalFilename();
            String filePath = uploadFileToS3(fileName, fotos);
            deleteBucketObject(depoimento);
            depoimento.setFotoPath(filePath);
        }
        Depoimentos depoimentoSalvo = depoimentoRepository.save(depoimento);
        UploadDepoimentosDto dpDto = new UploadDepoimentosDto(depoimentoSalvo.getNome(), depoimentoSalvo.getDepoimento(), depoimentoSalvo.getFotoPath());

        return dpDto;
    }

    public ResponseEntity<?> delete(Long id) {

        try {
            Optional<Depoimentos> depoimentoOptional = depoimentoRepository.findById(id);

            if (depoimentoOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Depoimentos depoimento = depoimentoOptional.get();
            deleteBucketObject(depoimento);
            depoimentoRepository.deleteById(id);

            return ResponseEntity.noContent().build();

        } catch (AmazonServiceException e) {
            log.error("Erro ao excluir objeto do bucket: " + e.getErrorMessage());
        }
        return ResponseEntity.noContent().build();
    }

    private void deleteBucketObject(Depoimentos depoimento) {
        String[] parts = depoimento.getFotoPath().split("/");
        String objetoKey = parts[parts.length - 1];

        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, objetoKey);
        s3Client.deleteObject(deleteObjectRequest);
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
            return "https://s3." + s3Client.getRegionName() + ".amazonaws.com/" + bucketName + "/" + fileName;
            //https://s3.region-code.amazonaws.com/bucket-name/key-name
        } catch (Exception e) {
            log.error("Erro ao enviar o arquivo para o s3", e);
            return null;
        }
    }
}
