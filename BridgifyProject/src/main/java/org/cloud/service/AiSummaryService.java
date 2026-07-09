package org.cloud.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.cloud.dto.AiSummaryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

/**
 * 시뮬레이션 결과를 Gemini AI로 해설하는 서비스.
 *
 * 보안: 프롬프트는 백엔드에서 조립한다. 프론트가 프롬프트 문자열을 그대로
 * 보내면 사용자가 임의의 지시를 주입할 수 있으므로(프롬프트 인젝션),
 * 프론트에서는 "숫자 데이터"만 받고 문장은 서버가 만든다.
 */
@Service
@RequiredArgsConstructor
public class AiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryService.class);

    private final RestTemplate restTemplate;
    private final InflationDataService inflationDataService;

    @Value("${api.gemini.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent";

    public String summarize(AiSummaryRequest req) {

        String prompt = buildPrompt(req);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(GEMINI_URL, entity, Map.class);

            return extractText(resp);

        } catch (Exception e) {
            log.warn("Gemini 요약 실패: {}", e.getMessage());
            return "AI 해설을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
        }
    }

    /** 시뮬 결과 숫자들로 프롬프트를 조립한다. */
    private String buildPrompt(AiSummaryRequest r) {
        BigDecimal usRate = inflationDataService.getUsInflationRate();
        BigDecimal krRate = inflationDataService.getKrInflationRate();

        StringBuilder sb = new StringBuilder();
        sb.append("당신은 장기 투자 데이터를 쉽게 풀어주는 재무 분석가입니다. ")
          .append("아래 시뮬레이션 결과를 한국어로 요약하세요.\n")
          .append("작성 규칙:\n")
          .append("- 반드시 두 문단으로 나누고, 문단 사이는 빈 줄 하나로 구분하세요.\n")
          .append("  · 첫 문단: 투자 원금이 얼마로 불어났는지 등 명목 결과 요약 (2~3문장)\n")
          .append("  · 둘째 문단: 실질 구매력이 낮아진 이유, 즉 세금과 물가의 영향 (2~3문장)\n")
          .append("- '고객님', '귀하' 같은 호칭이나 인사말을 쓰지 말고, 데이터를 담백하게 설명하세요.\n")
          .append("- 마크다운·목록 기호 없이 자연스러운 문장으로 쓰세요.\n")
          .append("- 숫자는 '억/만 원' 단위로 읽기 쉽게 표현하세요.\n")
          .append("- 특정 종목의 매수·매도 추천은 절대 하지 마세요.\n")
          .append("- 결과 해석과 물가·세금의 영향에 집중하세요.\n\n");

        sb.append("[투자 조건]\n");
        sb.append("- 투자 기간: ").append(r.getDurationYears()).append("년\n");
        sb.append("- 총 투자 원금: ").append(won(r.getTotalPrincipal())).append("\n");
        if (r.getTickers() != null && !r.getTickers().isEmpty()) {
            sb.append("- 보유 종목: ").append(String.join(", ", r.getTickers())).append("\n");
        }

        sb.append("\n[결과]\n");
        sb.append("- 최종 명목 자산: ").append(won(r.getNominalBalanceKrw())).append("\n");
        sb.append("- 실질 구매력 가치: ").append(won(r.getRealBalanceKrw())).append("\n");
        sb.append("- 총 투자 수익(명목): ").append(won(r.getTotalProfit())).append("\n");
        sb.append("- 연평균 수익률: ").append(r.getReturnRate()).append("%\n");
        if (r.getTax() != null) {
            sb.append("- 예상 세금: ").append(won(r.getTax())).append("\n");
        }

        sb.append("\n[물가 반영]\n");
        sb.append("- 미국 물가상승률 ").append(usRate).append("% (주가와 환율에 이미 반영됨)\n");
        sb.append("- 한국 물가상승률 ").append(krRate).append("% (최종 원화의 구매력 차감에 적용)\n");

        sb.append("\n명목 자산과 실질 구매력의 차이가 왜 생기는지, ")
          .append("물가와 세금이 각각 얼마나 영향을 줬는지 짚어 주세요.");

        return sb.toString();
    }

    private String won(BigDecimal v) {
        if (v == null) return "0원";
        return v.toBigInteger().toString() + "원";
    }

    /** Gemini 응답에서 텍스트만 뽑아낸다. */
    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> resp) {
        if (resp == null) return "AI 응답이 비어 있습니다.";

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) resp.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "AI 응답이 비어 있습니다.";

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) return "AI 응답이 비어 있습니다.";

        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "AI 응답이 비어 있습니다.";

        Object text = parts.get(0).get("text");
        return text != null ? text.toString().trim() : "AI 응답이 비어 있습니다.";
    }
}