package com.backend.nova.chat.controller;

import com.backend.nova.chat.dto.RagAdminQueryResponse;
import com.backend.nova.chat.dto.RagDocInput;
import com.backend.nova.chat.service.RagSeedService;
import com.backend.nova.chat.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "RAG Admin", description = "RAG(Pinecone) 문서 적재/삭제 관리 API")
@RestController
@RequestMapping("/admin/rag")
@RequiredArgsConstructor
public class RagAdminController {

    private final RagSeedService ragSeedService;
    private final RagService ragService;

    @Operation(
            summary = "RAG 검색(Query) - Pinecone TopK 조회",
            description = """
                Pinecone(Vector DB)에서 특정 아파트(apartmentId)의 문서들을 대상으로
                질의(q)를 임베딩한 뒤, 유사도 기반 TopK 결과를 조회합니다.

                - apartmentId: 아파트 구분 키 (Pinecone metadata filter)
                - sourceType: 문서 타입 필터 (GUIDE/EVENT/RULE/NOTICE/FAQ 등) [선택]
                - q: 검색 질의 텍스트(자연어)
                - topK: 반환할 최대 결과 수 (기본 5)

                반환값(hits)은 score(유사도), vectorId(Pinecone 벡터 ID), docId, sourceType, text(metadata)를 포함합니다.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RagAdminQueryResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 오류", content = @Content),
            @ApiResponse(responseCode = "500", description = "임베딩/검색 실패", content = @Content)
    })
    @GetMapping("/query")
    public RagAdminQueryResponse query(
            @Parameter(description = "대상 아파트 ID", example = "1", required = true)
            @RequestParam Long apartmentId,

            @Parameter(description = "문서 타입 필터 (GUIDE/EVENT/RULE/NOTICE/FAQ 등). 미입력 시 전체", example = "GUIDE")
            @RequestParam(required = false) String sourceType,

            @Parameter(description = "검색 질의(자연어)", example = "헬스장 운영시간 알려줘", required = true)
            @RequestParam String q,

            @Parameter(description = "TopK 결과 개수", example = "5")
            @RequestParam(defaultValue = "5") int topK
    ) {
        return ragService.adminQuery(apartmentId, sourceType, q, topK);
    }

    @Operation(
            summary = "RAG 문서 업서트(Seed) - 커스텀",
            description = """
                    DB와 무관하게 관리자가 직접 문서를 입력해 RAG(Pinecone)에 업서트합니다.
                    
                    - apartmentId: 아파트 구분 키 (검색 시 필터)
                    - sourceType: 문서 타입 (GUIDE/EVENT/RULE/FAQ 등)
                    - body: [{docId,title,content}] 배열
                    - 같은 (apartmentId, sourceType, docId)로 다시 업서트하면 덮어쓰기됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업서트 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터/바디 오류", content = @Content),
            @ApiResponse(responseCode = "500", description = "임베딩/벡터 업서트 실패", content = @Content)
    })
    @PostMapping("/seed/custom")
    public String seedCustom(
            @Parameter(description = "대상 아파트 ID", example = "1", required = true)
            @RequestParam Long apartmentId,

            @Parameter(description = "문서 타입 (GUIDE/EVENT/RULE/FAQ 등)", example = "GUIDE")
            @RequestParam(defaultValue = "GUIDE") String sourceType,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "RAG에 업서트할 문서 목록",
                    required = true,
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RagDocInput.class)))
            )
            @RequestBody List<RagDocInput> docs
    ) {
        int count = ragSeedService.seedApartmentDocs(apartmentId, sourceType, docs);
        return sourceType + " Seed 완료: " + count + " vectors";
    }

    @Operation(
            summary = "RAG 문서 삭제",
            description = """
                    Pinecone에서 특정 문서를 삭제합니다(청크 포함).
                    
                    - apartmentId + sourceType + docId 조합으로 필터 삭제합니다.
                    - 주의: Pinecone은 삭제 즉시 반영되나, 클라이언트 캐시/검색 결과 반영에 약간의 지연이 있을 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 오류", content = @Content),
            @ApiResponse(responseCode = "500", description = "삭제 실패", content = @Content)
    })
    @DeleteMapping("/docs")
    public String deleteDoc(
            @Parameter(description = "대상 아파트 ID", example = "1", required = true)
            @RequestParam Long apartmentId,

            @Parameter(description = "문서 타입 (GUIDE/EVENT/RULE/FAQ 등)", example = "GUIDE", required = true)
            @RequestParam String sourceType,

            @Parameter(description = "문서 식별자(docId)", example = "GUIDE_001", required = true)
            @RequestParam String docId
    ) {
        int deleted = ragSeedService.deleteApartmentDoc(apartmentId, sourceType, docId);
        return "삭제 완료: " + deleted + " vectors";
    }
}