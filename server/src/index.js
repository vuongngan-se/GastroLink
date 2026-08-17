const express = require("express");
const rateLimit = require("express-rate-limit");
const OpenAI = require("openai");
const db = require("./db");

const PORT = Number(process.env.PORT || 3000);
const MAX_REQUEST_SIZE = process.env.MAX_REQUEST_SIZE || "10mb";
const RATE_LIMIT_WINDOW_MS = Number(process.env.RATE_LIMIT_WINDOW_MS || 60_000);
const RATE_LIMIT_MAX_REQUESTS = Number(process.env.RATE_LIMIT_MAX_REQUESTS || 30);
const OPENAI_MODEL = process.env.OPENAI_MODEL || "llama-3.3-70b-versatile";
const TRUST_PROXY = (process.env.TRUST_PROXY || "false").toLowerCase() === "true";
const AI_PROXY_TOKEN = String(process.env.AI_PROXY_TOKEN || "").trim();

const ALLOWED_METHODS = ["GET", "POST", "OPTIONS"];
const ALLOWED_HEADERS = ["Content-Type", "Authorization", "X-API-KEY"];

const openai = new OpenAI({
  apiKey: process.env.GROQ_API_KEY || "dummy_key",
  baseURL: "https://api.groq.com/openai/v1"
});

const app = express();
app.disable("x-powered-by");

if (TRUST_PROXY) {
  app.set("trust proxy", 1);
}

app.use(createCorsMiddleware());
app.use(express.json({ limit: MAX_REQUEST_SIZE }));

const limiter = rateLimit({
  windowMs: RATE_LIMIT_WINDOW_MS,
  limit: RATE_LIMIT_MAX_REQUESTS,
  standardHeaders: true,
  legacyHeaders: false,
  statusCode: 429,
  message: {
    error: "Quá nhiều yêu cầu. Vui lòng thử lại sau vài giây."
  },
  handler: (_req, res) => {
    res.status(429).json({
      error: "Quá nhiều yêu cầu. Vui lòng thử lại sau vài giây."
    });
  }
});

app.use("/ai", limiter);

async function createChatCompletionWithFallback(options) {
  try {
    return await openai.chat.completions.create(options);
  } catch (error) {
    if (options.model !== "llama-3.1-8b-instant") {
      console.warn(`[Fallback] Lỗi khi sử dụng model ${options.model} (${error.message}). Tự động thử lại bằng model dự phòng llama-3.1-8b-instant...`);
      const fallbackOptions = { ...options, model: "llama-3.1-8b-instant" };
      if (fallbackOptions.max_tokens && fallbackOptions.max_tokens < 800) {
        fallbackOptions.max_tokens = 800;
      }
      return await openai.chat.completions.create(fallbackOptions);
    }
    throw error;
  }
}

app.get("/health", (_req, res) => {
  res.status(200).json({ status: "ok" });
});

function generateFallbackRecommendation(request) {
  const mode = request.orderMode === "SOLO" ? "bạn" : "nhóm của bạn";
  const dishes = Array.isArray(request.availableDishes) && request.availableDishes.length > 0
    ? request.availableDishes.slice(0, 2).map(d => d.name || d).join(" và ")
    : "Cá hồi nướng củ thì là và Cơm hải sản Tây Ban Nha";
    
  return `Tôi khuyên ${mode} nên chọn các món như ${dishes}. Đây là những lựa chọn vô cùng giàu dưỡng chất, cung cấp đầy đủ Protein lành mạnh và kiểm soát chất béo lý tưởng giúp duy trì năng lượng bền bỉ suốt cả ngày!`;
}

function generateFallbackChatReply(messages, profile, availableDishes) {
  const lastMessage = messages[messages.length - 1]?.content?.toLowerCase() || "";
  
  if (lastMessage.includes("chào") || lastMessage.includes("hello") || lastMessage.includes("hi")) {
    return "Xin chào! Tôi là Trợ lý Dinh dưỡng GastroLink. Tôi có thể giúp gì cho mục tiêu dinh dưỡng và thực đơn ăn uống của bạn hôm nay?";
  }
  
  if (lastMessage.includes("món") || lastMessage.includes("ăn gì") || lastMessage.includes("thực đơn") || lastMessage.includes("gợi ý") || lastMessage.includes("khuyên")) {
    if (Array.isArray(availableDishes) && availableDishes.length > 0) {
      const dishesStr = availableDishes.slice(0, 3).join(", ");
      return `Dựa trên thực đơn hôm nay, tôi khuyên bạn nên thưởng thức các món: ${dishesStr}. Đây là các món ăn vô cùng thơm ngon và cân đối lượng Calo lý tưởng!`;
    }
    return "Hôm nay tôi đặc biệt khuyên bạn dùng thử món Cá hồi nướng củ thì là & cà chua hoặc Bún Laksa Hải Sản. Cả hai món đều rất giàu đạm, ít chất béo bão hòa và cực kỳ ngon miệng!";
  }
  
  if (lastMessage.includes("calo") || lastMessage.includes("dinh dưỡng") || lastMessage.includes("béo") || lastMessage.includes("đạm") || lastMessage.includes("protein")) {
    return "Để quản lý cân nặng và duy trì cơ bắp tốt nhất, bạn nên ưu tiên món giàu Protein (như Cá hồi, Tôm) và nhiều chất xơ. Hãy hạn chế các món chiên ngập dầu và đồ uống nhiều đường tinh luyện nhé!";
  }
  
  if (lastMessage.includes("cảm ơn") || lastMessage.includes("thanks") || lastMessage.includes("ok")) {
    return "Rất sẵn lòng được đồng hành cùng bạn! Chúc bạn có một bữa ăn tràn đầy sức khỏe và ngon miệng cùng GastroLink nhé! ❤️";
  }
  
  if (profile && profile.summary) {
    return `Tôi đã ghi nhận mục tiêu dinh dưỡng của bạn. Để duy trì sức khỏe tốt nhất, hãy chọn những món ăn ít calo nhưng giàu đạm hôm nay nhé. Bạn có muốn tôi giới thiệu chi tiết một món nào không?`;
  }
  
  return "Tôi hiểu nhu cầu của bạn rồi ạ! Bạn có muốn tôi thiết kế một thực đơn riêng theo lượng calo mong muốn của bạn ngày hôm nay không?";
}

app.post("/ai/recommendation", requireProxyAuth, async (req, res) => {
  const validation = sanitizeAndValidateRequest(req.body);
  if (!validation.ok) {
    return res.status(400).json({ error: validation.error });
  }

  const safeRequest = validation.data;

  try {
    if (!process.env.GROQ_API_KEY || process.env.GROQ_API_KEY === "YOUR_GROQ_API_KEY_HERE" || process.env.GROQ_API_KEY === "dummy_key") {
      throw new Error("GROQ API Key chưa được cấu hình.");
    }

    const prompt = buildPrompt(safeRequest);

    const completion = await createChatCompletionWithFallback({
      model: OPENAI_MODEL,
      temperature: 0.2,
      max_tokens: 400,
      messages: [
        {
          role: "system",
          content:
            "Bạn là trợ lý dinh dưỡng cho GastroLink. Hãy phản hồi bằng tiếng Việt với các khuyến nghị rõ ràng, trung lập và có thể thực hiện được. Chỉ trả lời tối đa hai câu."
        },
        {
          role: "user",
          content: prompt
        }
      ]
    });

    const rawText = completion.choices?.[0]?.message?.content || "";
    const recommendationText = toTwoSentences(rawText);

    if (!recommendationText) {
      throw new Error("Không thể tạo đề xuất rỗng.");
    }

    return res.status(200).json({
      recommendationText,
      model: completion.model || OPENAI_MODEL,
      requestId: completion.id || null
    });
  } catch (_error) {
    console.warn("Lỗi tạo đề xuất (đang chuyển sang chế độ dự phòng):", _error.message);
    const recommendationText = generateFallbackRecommendation(safeRequest);
    return res.status(200).json({
      recommendationText,
      model: "fallback-model",
      requestId: "fallback-id"
    });
  }
});

app.post("/ai/chat", requireProxyAuth, async (req, res) => {
  const { messages, profile, availableDishes } = req.body || {};

  if (!Array.isArray(messages) || messages.length === 0 || messages.length > 40) {
    return res.status(400).json({ error: "messages không hợp lệ." });
  }

  const safeMessages = messages.map((m) => ({
    role: ["user", "assistant"].includes(m.role) ? m.role : "user",
    content: sanitizeText(String(m.content || ""), 600)
  })).filter((m) => m.content.length > 0);

  if (safeMessages.length === 0) {
    return res.status(400).json({ error: "Không có tin nhắn hợp lệ." });
  }

  const dishList = Array.isArray(availableDishes)
    ? availableDishes.slice(0, 30).map((d) => sanitizeText(String(d), 80)).filter(Boolean).join(", ")
    : "";

  const profileLine = profile && typeof profile === "object"
    ? `Thông tin dinh dưỡng người dùng (${sanitizeText(String(profile.type || ""), 30)}): ${sanitizeText(JSON.stringify(profile.summary || {}), 200)}`
    : "Không có thông tin dinh dưỡng người dùng.";

  const systemPrompt = [
    "Bạn là trợ lý ảo GastroLink, một ứng dụng đặt món ăn tích hợp theo dõi dinh dưỡng.",
    "Bạn giúp người dùng quyết định đặt món gì dựa trên sở thích và thông tin dinh dưỡng của họ.",
    "Hãy luôn phản hồi bằng tiếng Việt. Hãy ngắn gọn, thân thiện và thực tế.",
    "Không tự bịa ra các món ăn không có trong thực đơn sẵn có.",
    dishList ? `Thực đơn sẵn có: ${dishList}.` : "",
    profileLine
  ].filter(Boolean).join(" ");

  try {
    if (!process.env.GROQ_API_KEY || process.env.GROQ_API_KEY === "YOUR_GROQ_API_KEY_HERE" || process.env.GROQ_API_KEY === "dummy_key") {
      throw new Error("GROQ API Key chưa được cấu hình.");
    }

    const completion = await createChatCompletionWithFallback({
      model: OPENAI_MODEL,
      temperature: 0.6,
      max_tokens: 800,
      messages: [{ role: "system", content: systemPrompt }, ...safeMessages]
    });

    const reply = sanitizeText(completion.choices?.[0]?.message?.content || "", 3000);
    if (!reply) {
      throw new Error("Không thể tạo câu trả lời rỗng.");
    }

    return res.status(200).json({ reply });
  } catch (_error) {
    console.warn("Lỗi chat AI (đang chuyển sang chế độ dự phòng):", _error.message);
    const reply = generateFallbackChatReply(safeMessages, profile, availableDishes);
    return res.status(200).json({ reply });
  }
});

app.post("/ai/scan-dish", requireProxyAuth, async (req, res) => {
  const { image } = req.body || {};
  if (!image) {
    return res.status(400).json({ error: "Thiếu dữ liệu hình ảnh." });
  }

  // Chế độ mô phỏng hoặc phác họa nhanh bằng đề xuất
  if (image === "mock_salad" || image === "mock_salmon" || image === "mock_tacos" || 
      !process.env.GROQ_API_KEY || process.env.GROQ_API_KEY === "YOUR_GROQ_API_KEY_HERE" || process.env.GROQ_API_KEY === "dummy_key") {
    
    let mockResponse = {
      name: "Salad tôm tươi Bang Bang cay nồng",
      kcal: 320,
      proteinG: 24,
      carbsG: 15,
      fatG: 12,
      ingredients: ["Tôm sú tươi lột vỏ", "Xà lách", "Cà chua bi", "Sốt bơ lạc cay", "Ớt đỏ băm"],
      recipes: [
        "Công thức 1: Salad tôm sốt sữa chua Hy Lạp - Luộc chín tôm, trộn cùng xà lách tươi và sốt sữa chua Hy Lạp ít béo để giảm calo.",
        "Công thức 2: Tôm áp chảo muối ớt - Áp chảo tôm với 1 thìa dầu ô liu và bột ớt cay, ăn kèm rau luộc để tăng xơ."
      ]
    };

    if (image === "mock_salmon") {
      mockResponse = {
        name: "Cá hồi nướng củ thì là & cà chua",
        kcal: 450,
        proteinG: 35,
        carbsG: 12,
        fatG: 28,
        ingredients: ["Cá hồi phi lê", "Thì là tươi", "Cà chua bi", "Dầu ô liu", "Gia vị thảo mộc"],
        recipes: [
          "Công thức 1: Cá hồi áp chảo sốt chanh leo - Áp chảo cá hồi với tỏi, rim lửa nhỏ cùng nước cốt chanh leo chua ngọt tự nhiên.",
          "Công thức 2: Cháo cá hồi yến mạch - Nấu yến mạch cùng cá hồi phi lê dầm nhỏ, hành lá và gừng tươi giúp giữ ấm bụng."
        ]
      };
    } else if (image === "mock_tacos") {
      mockResponse = {
        name: "Bánh kẹp Tacos cá chiên sốt Cajun",
        kcal: 410,
        proteinG: 22,
        carbsG: 38,
        fatG: 18,
        ingredients: ["Bánh Tortilla dẻo", "Cá basa chiên giòn", "Gia vị Cajun", "Cải bắp thái sợi", "Sốt kem chua"],
        recipes: [
          "Công thức 1: Tacos cá áp chảo ăn kiêng - Áp chảo cá thay vì chiên ngập dầu, dùng bánh tortilla lúa mạch nguyên cám.",
          "Công thức 2: Salad tacos cá - Bỏ vỏ bánh Tortilla, trộn thịt cá áp chảo cùng bắp cải thái nhỏ và sốt sữa chua tỏi."
        ]
      };
    } else if (image !== "mock_salad") {
      const fallbacks = [
        {
          name: "Cá hồi áp chảo sốt Teriyaki mật ong",
          kcal: 430,
          proteinG: 32,
          carbsG: 16,
          fatG: 22,
          ingredients: ["Cá hồi phi lê", "Mật ong rừng", "Sốt Teriyaki", "Gừng tươi", "Vừng rang"],
          recipes: [
            "Công thức 1: Cá hồi áp chảo sốt Teriyaki ít ngọt - Giảm lượng mật ong, áp chảo lửa nhỏ ăn kèm bông cải xanh luộc.",
            "Công thức 2: Cá hồi áp chảo tỏi ớt - Áp chảo lửa lớn với tỏi băm và ớt tươi giúp khử tanh, tăng cường chuyển hóa."
          ]
        },
        {
          name: "Cá chẽm nướng gia vị Ma-rốc",
          kcal: 380,
          proteinG: 30,
          carbsG: 10,
          fatG: 15,
          ingredients: ["Cá chẽm phi lê", "Gia vị Ma-rốc", "Chanh tươi", "Rau mùi Tây", "Tỏi băm"],
          recipes: [
            "Công thức 1: Cá chẽm cuộn giấy bạc nướng - Nướng cùng gia vị Ma-rốc, hành tây và nấm đùi gà để giữ trọn vị ngọt tự nhiên.",
            "Công thức 2: Cá chẽm sốt cà chua thảo mộc - Áp chảo sơ cá, sốt cùng cà chua tươi xay nhuyễn và tỏi thơm."
          ]
        }
      ];
      mockResponse = fallbacks[Math.floor(Math.random() * fallbacks.length)];
    }

    return res.status(200).json(mockResponse);
  }

  try {
    const completion = await openai.chat.completions.create({
      model: "meta-llama/llama-4-scout-17b-16e-instruct",
      messages: [
        {
          role: "user",
          content: [
            {
              type: "text",
              text: "Phân tích món ăn hoặc nguyên liệu trong ảnh này. Dịch tên món ăn và nguyên liệu sang Tiếng Việt. Nếu phát hiện đây là nguyên liệu thô/sống (như thịt gà sống, tôm sống, rau quả tươi), hãy đề xuất 2 công thức chế biến lành mạnh nhất trong trường 'recipes'. Trả về duy nhất một chuỗi JSON hợp lệ theo đúng cấu trúc sau (không viết thêm bất kỳ từ giải thích nào ngoài JSON): {\"name\":\"Tên món ăn hoặc nguyên liệu bằng tiếng Việt\",\"kcal\":450,\"proteinG\":35,\"carbsG\":12,\"fatG\":28,\"ingredients\":[\"Nguyên liệu 1\",\"Nguyên liệu 2\"],\"recipes\":[\"Công thức 1: Tên món - Cách làm nhanh tốt cho sức khỏe\",\"Công thức 2: Tên món - Cách làm nhanh tốt cho sức khỏe\"]}"
            },
            {
              type: "image_url",
              image_url: {
                url: `data:image/jpeg;base64,${image}`
              }
            }
          ]
        }
      ],
      temperature: 0.2
    });

    const rawText = completion.choices?.[0]?.message?.content || "";
    const jsonMatch = rawText.match(/\{[\s\S]*\}/);
    if (!jsonMatch) {
      throw new Error("Không thể phân tích JSON từ AI");
    }

    const result = JSON.parse(jsonMatch[0]);
    return res.status(200).json({
      name: String(result.name || "Món ăn dinh dưỡng"),
      kcal: Number(result.kcal || 350),
      proteinG: Number(result.proteinG || result.protein_g || 20),
      carbsG: Number(result.carbsG || result.carbs_g || 30),
      fatG: Number(result.fatG || result.fat_g || 10),
      ingredients: Array.isArray(result.ingredients) ? result.ingredients : ["Hải sản tươi", "Gia vị tổng hợp"],
      recipes: Array.isArray(result.recipes) ? result.recipes : []
    });
  } catch (error) {
    console.error("Lỗi phân tích ảnh vision (dùng fallback):", error);
    return res.status(200).json({
      name: "Salad tôm tươi Bang Bang cay nồng",
      kcal: 320,
      proteinG: 24,
      carbsG: 15,
      fatG: 12,
      ingredients: ["Tôm sú tươi lột vỏ", "Xà lách", "Cà chua bi", "Sốt bơ lạc cay", "Ớt đỏ băm"],
      recipes: [
        "Công thức 1: Salad tôm sốt sữa chua Hy Lạp - Luộc chín tôm, trộn cùng xà lách tươi và sốt sữa chua Hy Lạp ít béo để giảm calo.",
        "Công thức 2: Tôm áp chảo muối ớt - Áp chảo tôm với 1 thìa dầu ô liu và bột ớt cay, ăn kèm rau luộc để tăng xơ."
      ]
    });
  }
});

const BRANCHES_DATA = [
  { "id": "b001", "name": "GastroLink Trung tâm", "city": "Hà Nội" },
  { "id": "b002", "name": "GastroLink Phía Bắc", "city": "Hải Phòng" },
  { "id": "b003", "name": "GastroLink Hải cảng", "city": "Đà Nẵng" },
  { "id": "b004", "name": "GastroLink Phía Nam", "city": "TP. Hồ Chí Minh" }
];

const FALLBACK_TRANSLATIONS = {
  "Baked salmon with fennel & tomatoes": {
    name: "Cá hồi nướng củ thì là & cà chua",
    ingredients: ["Cá hồi phi lê", "Thì là tươi", "Cà chua bi", "Dầu ô liu", "Gia vị thảo mộc"]
  },
  "Arroz con gambas y calamar": {
    name: "Cơm hải sản mực và tôm Tây Ban Nha",
    ingredients: ["Gạo nở hạt tròn", "Tôm tươi lột vỏ", "Mực ống tươi", "Nghêu biển", "Bột nghệ tây"]
  },
  "Fish Pie": {
    name: "Bánh nướng nhân cá tuyết & phô mai",
    ingredients: ["Cá thu phi lê", "Khoai tây nghiền", "Phô mai Cheddar", "Sữa kem tươi"]
  },
  "Garides Saganaki": {
    name: "Tôm sốt cà chua Hy Lạp & phô mai Feta",
    ingredients: ["Tôm sú tươi ngon", "Sốt cà chua đậm vị", "Phô mai Feta", "Tỏi băm", "Húng quế Tây"]
  },
  "Kafteji": {
    name: "Kafteji trứng mực & rau củ chiên",
    ingredients: ["Lòng đỏ trứng gà", "Khoai tây chiên", "Bí ngòi chiên", "Tôm thịt chín", "Gia vị cay"]
  },
  "Laksa": {
    name: "Bún Laksa hải sản Singapore",
    ingredients: ["Sợi bún Laksa", "Tôm sú tươi", "Đậu hũ chiên phồng", "Giá đỗ sạch", "Nước cốt dừa béo"]
  },
  "Bang bang prawn salad": {
    name: "Salad tôm tươi Bang Bang cay nồng",
    ingredients: ["Tôm tươi lột vỏ", "Xà lách", "Cà chua bi", "Sốt bơ lạc cay", "Ớt đỏ băm"]
  },
  "Barramundi with Moroccan spices": {
    name: "Cá chẽm nướng gia vị Ma-rốc",
    ingredients: ["Cá chẽm phi lê", "Gia vị Ma-rốc", "Chanh tươi", "Rau mùi Tây", "Tỏi băm"]
  },
  "Cajun spiced fish tacos": {
    name: "Bánh kẹp Tacos cá chiên sốt Cajun",
    ingredients: ["Bánh Tortilla dẻo", "Cá basa chiên giòn", "Gia vị Cajun", "Cải bắp thái sợi", "Sốt kem chua"]
  },
  "Clam, chorizo & white bean stew": {
    name: "Súp hầm nghêu, xúc xích Chorizo & đậu trắng",
    ingredients: ["Nghêu tươi sạch", "Xúc xích Chorizo", "Đậu trắng hạt", "Cà chua bi", "Hành tây", "Tỏi băm"]
  },
  "Escabeche de Pescado": {
    name: "Cá chiên sốt giấm ớt Nam Mỹ (Escabeche)",
    ingredients: ["Cá thu phi lê", "Ớt chuông ngọt", "Giấm rượu đỏ", "Hành tây tím", "Gia vị thảo mộc Nam Mỹ"]
  },
  "Grilled Portuguese Sardines": {
    name: "Cá mòi nướng muối tiêu kiểu Bồ Đào Nha",
    ingredients: ["Cá mòi tươi nguyên con", "Dầu ô liu", "Muối biển hột", "Chanh tươi lát", "Tỏi băm nhuyễn"]
  },
  "Honey Teriyaki Salmon": {
    name: "Cá hồi áp chảo sốt Teriyaki mật ong",
    ingredients: ["Cá hồi phi lê", "Mật ong rừng", "Sốt tương đậm vị Teriyaki", "Gừng băm tươi", "Vừng trắng rang"]
  },
  "Kedgeree": {
    name: "Cơm Kedgeree cá hồi & trứng luộc kiểu Anh",
    ingredients: ["Gạo nở hạt dài", "Cá hồi phi lê chín xé", "Trứng gà luộc chín", "Bột cà ri Ấn Độ", "Hành tây phi thơm"]
  },
  "Kung Pao Prawns": {
    name: "Tôm sú xào cung bảo hạt điều cay ngọt",
    ingredients: ["Tôm sú lột vỏ", "Ớt khô đỏ", "Hạt điều rang giòn", "Sốt dầu hào Kung Pao", "Hành lá cắt khúc"]
  },
  "Oysters Rockefeller": {
    name: "Hàu nướng phô mai đút lò Rockefeller",
    ingredients: ["Hàu sữa tươi béo ngậy", "Bơ nhạt", "Rau cải chân vịt", "Phô mai Parmesan bột", "Vụn bánh mì giòn"]
  },
  "Salmon Avocado Salad": {
    name: "Salad cá hồi áp chảo quả bơ chín",
    ingredients: ["Cá hồi phi lê áp chảo", "Quả bơ chín béo", "Rau xà lách sạch", "Sốt mù tạt mật ong", "Cà chua bi tươi"]
  },
  "Salmon Florentine": {
    name: "Cá hồi áp chảo sốt kem phô mai Florentine",
    ingredients: ["Cá hồi phi lê", "Cải bó xôi (chân vịt)", "Kem béo tươi (Whipping Cream)", "Tỏi băm", "Phô mai Parmesan bào"]
  },
  "Shrimp Creole": {
    name: "Tôm sú sốt cay kiểu Creole miền Nam Mỹ",
    ingredients: ["Tôm sú tươi lột vỏ", "Sốt cà chua nghiền", "Ớt chuông xanh đỏ", "Nhánh cần tây", "Bột ớt Creole đặc trưng"]
  },
  // --- Món Gà (Chicken) ---
  "Chicken & Mushroom Hotpot": {
    name: "Lẩu gà hầm nấm hương ấm nồng",
    ingredients: ["Thịt đùi gà ta", "Nấm hương tươi", "Cà rốt", "Kỷ tử", "Gừng tươi", "Nước dùng xương hầm"]
  },
  "Chicken Jalfrezi": {
    name: "Cà ri gà Jalfrezi cay vừa kiểu Ấn",
    ingredients: ["Thịt ức gà", "Ớt chuông đỏ xanh", "Hành tây", "Cà chua nghiền", "Bột gia vị Jalfrezi", "Ngò rí"]
  },
  "Chicken Katsu": {
    name: "Cơm gà chiên xù Katsu Nhật Bản",
    ingredients: ["Thịt ức gà", "Bột chiên xù xơ dừa", "Trứng gà", "Nước sốt Katsu đậm đà", "Bắp cải bào sợi"]
  },
  "Chicken Marengo": {
    name: "Gà hầm nấm Marengo kiểu Pháp",
    ingredients: ["Thịt gà ta chặt miếng", "Cà chua nghiền sốt", "Nấm mỡ tươi", "Rượu vang trắng", "Tỏi", "Lá nguyệt quế"]
  },
  "Coq au Vin": {
    name: "Gà sốt vang đỏ Coq au Vin kiểu Pháp",
    ingredients: ["Đùi cánh gà ta", "Rượu vang đỏ đằm", "Thịt ba chỉ xông khói", "Nấm mỡ", "Hành tây bi", "Tỏi băm"]
  },
  "Tandoori Chicken": {
    name: "Gà nướng lò đất Tandoori Ấn Độ",
    ingredients: ["Đùi tỏi gà lớn", "Sữa chua không đường", "Bột gia vị Tandoori masala", "Chanh tươi", "Tỏi gừng giã nhuyễn"]
  },
  // --- Món Bò (Beef) ---
  "Beef Lo Mein": {
    name: "Mỳ xào thịt bò và rau cải ngũ sắc",
    ingredients: ["Mỳ sợi trứng", "Thịt thăn bò tươi", "Ớt chuông ngọt", "Súp lơ xanh", "Sốt tương đen hảo hạng", "Tỏi băm"]
  },
  "Beef Wellington": {
    name: "Thịt bò nướng lá bột Wellington Thượng hạng",
    ingredients: ["Thịt thăn nội bò Mỹ", "Nấm hương băm nhuyễn", "Vỏ bánh ngàn lớp dẻo", "Phết lòng đỏ trứng", "Lá ngải cứu Tây"]
  },
  "Beef and Mustard Pie": {
    name: "Bánh nướng nhân thịt bò sốt mù tạt kiểu Anh",
    ingredients: ["Thịt vai bò thái hạt lựu", "Mù tạt Dijon vàng", "Lá bột bánh nướng giòn", "Hành tây tím", "Nước hầm bò"]
  },
  "Beef and Oyster Pie": {
    name: "Bánh nướng nhân thịt bò và hàu sữa béo",
    ingredients: ["Thịt thăn bò", "Hàu sữa tươi sạch", "Vỏ bánh nướng dẻo phồng", "Hành tím phi", "Gia vị thảo mộc"]
  },
  "Beef Bourguignon": {
    name: "Thịt bò hầm vang đỏ Bourguignon thơm lừng",
    ingredients: ["Thịt bắp bò hoa", "Rượu vang đỏ đậm", "Cà rốt củ", "Nấm tươi", "Tỏi", "Nhánh hương thảo"]
  },
  "Beef Stroganoff": {
    name: "Thịt bò xào sốt kem nấm Stroganoff nước Nga",
    ingredients: ["Thịt thăn bò thái mỏng", "Nấm mỡ cắt lát", "Kem chua Sour Cream", "Hành tây phi", "Bơ lạt thơm"]
  },
  // --- Món Chay (Vegetarian) ---
  "Dal fry": {
    name: "Súp đậu lăng Dal Fry bơ ngậy Ấn Độ",
    ingredients: ["Đậu lăng đỏ hạt", "Bơ lạt Ghee", "Bột nghệ vàng", "Hạt cumin (thì là Ai Cập)", "Cà chua", "Ngò rí"]
  },
  "Egg Plants Brown Stew": {
    name: "Cà tím kho tương đen cay ngọt đậm đà",
    ingredients: ["Cà tím quả dài", "Sốt tương đen béo", "Ớt hiểm đỏ", "Hành lá", "Tỏi băm", "Dầu mè thơm"]
  },
  "Matar Paneer": {
    name: "Cà ri phô mai Paneer tươi và đậu hà lan",
    ingredients: ["Phô mai Paneer cắt khối", "Đậu hà lan hạt tròn", "Sốt cà ri hạt điều béo", "Gừng tỏi băm", "Rau mùi"]
  },
  "Mushroom & Chestnut Rotolo": {
    name: "Bánh cuộn nấm và hạt dẻ đút lò phô mai",
    ingredients: ["Tấm mỳ ống Ý", "Nấm mỡ", "Hạt dẻ bùi ngọt", "Phô mai Ricotta béo", "Phô mai Mozzarella kéo sợi"]
  },
  "Ratatouille": {
    name: "Rau củ hầm Ratatouille trứ danh nước Pháp",
    ingredients: ["Bí ngòi xanh", "Cà tím quả", "Ớt chuông đỏ vàng", "Sốt cà chua nghiền mịn", "Lá húng tây thơm"]
  },
  "Spinach & Ricotta Cannelloni": {
    name: "Mỳ ống Ý nhân cải bó xôi và phô mai Ricotta",
    ingredients: ["Ống mỳ Ý Cannelloni", "Rau cải bó xôi băm nhỏ", "Phô mai Ricotta thơm mịn", "Sốt kem béo béchamel"]
  }
};

app.get("/catalog/branches", requireProxyAuth, (req, res) => {
  res.status(200).json(BRANCHES_DATA);
});

app.get("/proxy-image", async (req, res) => {
  try {
    const imageUrl = req.query.url;
    if (!imageUrl) {
      return res.status(400).send("Thiếu tham số url");
    }

    const response = await fetch(imageUrl);
    if (!response.ok) {
      throw new Error("Không thể tải hình ảnh từ máy chủ ngoài");
    }

    const contentType = response.headers.get("content-type") || "image/jpeg";
    res.setHeader("Content-Type", contentType);
    res.setHeader("Cache-Control", "public, max-age=86400");

    const arrayBuffer = await response.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);
    res.send(buffer);
  } catch (error) {
    console.error("Lỗi khi proxy ảnh:", error);
    res.status(502).send("Lỗi tải hình ảnh");
  }
});

app.get("/catalog/external-dishes", requireProxyAuth, async (req, res) => {
  const calculateDishPrice = (dishName) => {
    let hash = 0;
    for (let i = 0; i < dishName.length; i++) {
      hash = dishName.charCodeAt(i) + ((hash << 5) - hash);
    }
    const minPrice = 45000;
    const maxPrice = 120000;
    const step = 5000;
    const range = (maxPrice - minPrice) / step;
    const price = minPrice + (Math.abs(hash) % (range + 1)) * step;
    return price;
  };

  try {
    const categories = ["Seafood", "Chicken", "Beef", "Vegetarian"];
    const fetchPromises = categories.map(cat =>
      fetch(`https://www.themealdb.com/api/json/v1/1/filter.php?c=${cat}`)
        .then(r => r.ok ? r.json() : { meals: [] })
        .catch(() => ({ meals: [] }))
    );
    const results = await Promise.all(fetchPromises);

    const rawMeals = [];
    results.forEach(resData => {
      if (resData && Array.isArray(resData.meals)) {
        // Giảm số lượng xuống 4 món mỗi loại để tải nhanh hơn
        rawMeals.push(...resData.meals.slice(0, 4));
      }
    });

    if (rawMeals.length === 0) {
      throw new Error("Không tìm thấy món ăn nào");
    }

    const host = req.get("host") || "10.0.2.2:3000";
    const protocol = req.protocol || "http";

    // Thực hiện song song việc tải chi tiết món ăn và dịch thuật/dự phòng
    const dishPromises = rawMeals.map(async (meal) => {
      const proxiedImageUrl = `${protocol}://${host}/proxy-image?url=${encodeURIComponent(meal.strMealThumb || "")}`;
      
      try {
        // 1. Kiểm tra xem món ăn đã có trong danh sách dịch thuật sẵn có chưa (để tránh gọi AI siêu chậm)
        const fallback = FALLBACK_TRANSLATIONS[meal.strMeal];
        if (fallback) {
          return {
            id: `ext_${meal.idMeal}`,
            name: fallback.name,
            kcal: fallback.kcal || 400,
            proteinG: fallback.proteinG || 28,
            protein_g: fallback.proteinG || 28,
            carbsG: fallback.carbsG || 38,
            carbs_g: fallback.carbsG || 38,
            fatG: fallback.fatG || 15,
            fat_g: fallback.fatG || 15,
            price: calculateDishPrice(fallback.name),
            imageUrl: proxiedImageUrl,
            image_url: proxiedImageUrl,
            ingredients: fallback.ingredients
          };
        }

        // 2. Nếu không có sẵn, tải chi tiết và thử dùng AI dịch thuật
        const detailRes = await fetch(`https://www.themealdb.com/api/json/v1/1/lookup.php?i=${meal.idMeal}`);
        if (!detailRes.ok) throw new Error("Không thể tải chi tiết món ăn");
        const detailData = await detailRes.json();
        const detail = detailData.meals?.[0];
        if (!detail) throw new Error("Không tìm thấy chi tiết");

        const rawIngredients = [];
        for (let i = 1; i <= 8; i++) {
          const ing = detail[`strIngredient${i}`];
          if (ing && ing.trim()) {
            rawIngredients.push(ing.trim());
          }
        }

        if (!process.env.GROQ_API_KEY || process.env.GROQ_API_KEY === "YOUR_GROQ_API_KEY_HERE" || process.env.GROQ_API_KEY === "dummy_key") {
          throw new Error("GROQ API Key chưa được cấu hình. Sử dụng dịch thuật dự phòng.");
        }

        const prompt = `Phân tích món ăn sau từ tiếng Anh:
Tên món: "${detail.strMeal}"
Nguyên liệu: ${JSON.stringify(rawIngredients)}

Hãy dịch tên món, nguyên liệu sang Tiếng Việt và ước lượng Calo (kcal), Protein (g), Carbs (g), Fat (g).
Trả về kết quả DUY NHẤT dưới dạng JSON hợp lệ theo đúng mẫu sau (không giải thích thêm):
{
  "name": "Tên tiếng việt của món",
  "kcal": 450,
  "protein_g": 30,
  "carbs_g": 40,
  "fat_g": 15,
  "ingredients": ["Nguyên liệu 1", "Nguyên liệu 2"]
}`;

        const completion = await createChatCompletionWithFallback({
          model: OPENAI_MODEL,
          temperature: 0.1,
          max_tokens: 220,
          messages: [
            {
              role: "user",
              content: prompt
            }
          ]
        });

        const replyContent = completion.choices?.[0]?.message?.content || "";
        const cleanContent = replyContent.substring(replyContent.indexOf("{"), replyContent.lastIndexOf("}") + 1);
        const aiJson = JSON.parse(cleanContent.trim());

        return {
          id: `ext_${detail.idMeal}`,
          name: aiJson.name || detail.strMeal,
          kcal: aiJson.kcal || 380,
          proteinG: aiJson.protein_g || 25,
          protein_g: aiJson.protein_g || 25,
          carbsG: aiJson.carbs_g || 35,
          carbs_g: aiJson.carbs_g || 35,
          fatG: aiJson.fat_g || 12,
          fat_g: aiJson.fat_g || 12,
          price: calculateDishPrice(aiJson.name || detail.strMeal),
          imageUrl: proxiedImageUrl,
          image_url: proxiedImageUrl,
          ingredients: aiJson.ingredients || rawIngredients
        };
      } catch (innerError) {
        console.warn(`Lỗi xử lý món ăn lẻ (đang dùng chế độ dự phòng): ${meal.strMeal}`, innerError.message);
        
        const fallback = FALLBACK_TRANSLATIONS[meal.strMeal] || {
          name: meal.strMeal,
          ingredients: ["Hải sản tươi", "Gia vị tổng hợp"]
        };

        return {
          id: `ext_${meal.idMeal}`,
          name: fallback.name,
          kcal: 400,
          proteinG: 28,
          protein_g: 28,
          carbsG: 38,
          carbs_g: 38,
          fatG: 15,
          fat_g: 15,
          price: calculateDishPrice(fallback.name),
          imageUrl: proxiedImageUrl,
          image_url: proxiedImageUrl,
          ingredients: fallback.ingredients
        };
      }
    });

    const dishesResults = await Promise.all(dishPromises);
    const dishes = dishesResults.filter(Boolean);

    res.status(200).json(dishes);
  } catch (error) {
    res.status(502).json({ error: "Lỗi tải món ăn từ máy chủ ngoài: " + error.message });
  }
});

// POST /ai/sync/upload - Đồng bộ dữ liệu lên MySQL
app.post("/ai/sync/upload", requireProxyAuth, async (req, res) => {
  const { userId, profile, orders, savedProfiles } = req.body;
  
  if (!db.isDbAvailable()) {
    return res.status(503).json({ error: "Cơ sở dữ liệu MySQL hiện không hoạt động." });
  }

  try {
    if (profile) {
      await db.saveProfile({
        userId: userId || "demo_user",
        age: profile.age,
        sex: profile.sex,
        weight: profile.weight,
        height: profile.height,
        goal: profile.goal,
        activityLevel: profile.activityLevel,
        allergies: profile.allergies,
        savedProfilesJson: Array.isArray(savedProfiles) ? JSON.stringify(savedProfiles) : "[]"
      });
    }

    if (Array.isArray(orders) && orders.length > 0) {
      await db.saveOrders(userId || "demo_user", orders);
    }

    res.status(200).json({ status: "success", message: "Đồng bộ dữ liệu lên Đám mây thành công!" });
  } catch (error) {
    console.error("Lỗi đồng bộ lên MySQL:", error);
    res.status(500).json({ error: "Lỗi đồng bộ dữ liệu: " + error.message });
  }
});

// GET /ai/sync/download - Tải dữ liệu từ MySQL về App
app.get("/ai/sync/download", requireProxyAuth, async (req, res) => {
  const userId = req.query.userId || "demo_user";

  if (!db.isDbAvailable()) {
    return res.status(503).json({ error: "Cơ sở dữ liệu MySQL hiện không hoạt động." });
  }

  try {
    const profile = await db.getProfile(userId);
    const orders = await db.getOrders(userId);

    let savedProfiles = [];
    if (profile && profile.savedProfilesJson) {
      try {
        savedProfiles = JSON.parse(profile.savedProfilesJson);
      } catch (_) {}
    }

    res.status(200).json({
      status: "success",
      profile: profile ? {
        age: profile.age,
        sex: profile.sex,
        weight: profile.weight,
        height: profile.height,
        goal: profile.goal,
        activityLevel: profile.activityLevel,
        allergies: profile.allergies
      } : null,
      savedProfiles,
      orders
    });
  } catch (error) {
    console.error("Lỗi tải dữ liệu từ MySQL:", error);
    res.status(500).json({ error: "Lỗi tải dữ liệu: " + error.message });
  }
});

app.use((req, res) => {
  res.status(404).json({ error: `Không tìm thấy đường dẫn: ${req.method} ${req.path}` });
});

app.use((err, _req, res, _next) => {
  if (err && err.type === "entity.too.large") {
    return res.status(413).json({
      error: "Nội dung yêu cầu vượt quá kích thước tối đa cho phép."
    });
  }

  return res.status(500).json({ error: "Lỗi máy chủ nội bộ." });
});

app.listen(PORT, async () => {
  console.log(`Proxy AI của GastroLink đang lắng nghe ở cổng ${PORT}`);
  await db.initDb();
});

function sanitizeAndValidateRequest(body) {
  if (!body || typeof body !== "object" || Array.isArray(body)) {
    return { ok: false, error: "Dữ liệu yêu cầu không hợp lệ." };
  }

  const orderMode = sanitizeText(body.orderMode, 20);
  const nutritionMode = sanitizeText(body.nutritionMode, 30);

  if (!orderMode || !nutritionMode) {
    return { ok: false, error: "Yêu cầu có orderMode và nutritionMode." };
  }

  if (!["SOLO", "GROUP"].includes(orderMode)) {
    return { ok: false, error: "orderMode không hợp lệ." };
  }

  if (!["WITH_PROFILE", "WITHOUT_PROFILE"].includes(nutritionMode)) {
    return { ok: false, error: "nutritionMode không hợp lệ." };
  }

  const totals = sanitizeTotals(body.totals);
  if (!totals) {
    return { ok: false, error: "totals không hợp lệ." };
  }

  const dishes = sanitizeDishes(body.dishes);
  if (!dishes) {
    return { ok: false, error: "dishes không hợp lệ." };
  }

  const profile = sanitizeProfile(body.profile);
  if (body.profile !== undefined && profile === null) {
    return { ok: false, error: "profile không hợp lệ." };
  }

  return {
    ok: true,
    data: {
      orderMode,
      nutritionMode,
      totals,
      dishes,
      profile
    }
  };
}

function sanitizeTotals(input) {
  if (!input || typeof input !== "object" || Array.isArray(input)) {
    return null;
  }

  const kcal = toSafeNumber(input.kcal);
  const proteinG = toSafeNumber(input.proteinG);
  const carbsG = toSafeNumber(input.carbsG);
  const fatG = toSafeNumber(input.fatG);

  const values = [kcal, proteinG, carbsG, fatG];
  if (values.some((value) => value === null)) {
    return null;
  }

  return {
    kcal,
    proteinG,
    carbsG,
    fatG
  };
}

function sanitizeDishes(input) {
  if (!Array.isArray(input) || input.length === 0 || input.length > 50) {
    return null;
  }

  const dishes = [];

  for (const item of input) {
    if (!item || typeof item !== "object" || Array.isArray(item)) {
      return null;
    }

    const name = sanitizeText(item.name, 80);
    const qty = toSafeNumber(item.qty);
    const kcal = toSafeNumber(item.kcal);
    const proteinG = toSafeNumber(item.proteinG);
    const carbsG = toSafeNumber(item.carbsG);
    const fatG = toSafeNumber(item.fatG);

    if (!name || [qty, kcal, proteinG, carbsG, fatG].some((v) => v === null)) {
      return null;
    }

    dishes.push({ name, qty, kcal, proteinG, carbsG, fatG });
  }

  return dishes;
}

function sanitizeProfile(input) {
  if (input === undefined || input === null) {
    return null;
  }

  if (typeof input !== "object" || Array.isArray(input)) {
    return null;
  }

  const type = sanitizeText(input.type, 30);
  if (!type) {
    return null;
  }

  const summary = sanitizeJsonValue(input.summary, 0);

  return {
    type,
    summary
  };
}

function sanitizeJsonValue(value, depth) {
  if (depth > 4) {
    return null;
  }

  if (value === null || value === undefined) {
    return null;
  }

  if (typeof value === "string") {
    return sanitizeText(value, 120);
  }

  if (typeof value === "number") {
    return toSafeNumber(value);
  }

  if (typeof value === "boolean") {
    return value;
  }

  if (Array.isArray(value)) {
    return value.slice(0, 20).map((item) => sanitizeJsonValue(item, depth + 1));
  }

  if (typeof value === "object") {
    const output = {};
    const entries = Object.entries(value).slice(0, 30);

    for (const [rawKey, rawValue] of entries) {
      const key = sanitizeText(rawKey, 40);
      if (!key) {
        continue;
      }
      output[key] = sanitizeJsonValue(rawValue, depth + 1);
    }

    return output;
  }

  return null;
}

function toSafeNumber(value) {
  const numberValue = Number(value);
  if (!Number.isFinite(numberValue)) {
    return null;
  }

  if (numberValue < 0 || numberValue > 100000) {
    return null;
  }

  return Math.round(numberValue * 100) / 100;
}

function sanitizeText(value, maxLength) {
  if (typeof value !== "string") {
    return "";
  }

  const cleaned = value
    .replace(/[\u0000-\u001F\u007F]/g, " ")
    .replace(/[<>`]/g, "")
    .replace(/\s+/g, " ")
    .trim();

  if (!cleaned) {
    return "";
  }

  return cleaned.slice(0, maxLength);
}

function buildPrompt(request) {
  const dishLines = request.dishes
    .map((dish, index) => {
      return `${index + 1}. ${dish.name} x${dish.qty} (kcal ${dish.kcal}, P ${dish.proteinG}g, C ${dish.carbsG}g, G ${dish.fatG}g)`;
    })
    .join("\n");

  const profileLine = request.profile
    ? `Thông tin (${request.profile.type}): ${JSON.stringify(request.profile.summary)}`
    : "Thông tin: không khả dụng";

  return [
    "Tạo một đề xuất dinh dưỡng ngắn gọn cho đơn hàng này.",
    "Giới hạn nghiêm ngặt: tối đa 2 câu.",
    `Chế độ đặt hàng: ${request.orderMode}`,
    `Chế độ dinh dưỡng: ${request.nutritionMode}`,
    `Tổng cộng: kcal ${request.totals.kcal}, P ${request.totals.proteinG}g, C ${request.totals.carbsG}g, G ${request.totals.fatG}g`,
    profileLine,
    "Món ăn:",
    dishLines
  ].join("\n");
}

function toTwoSentences(text) {
  const safeText = sanitizeText(String(text || ""), 1000);
  if (!safeText) {
    return "";
  }

  const parts = safeText
    .split(/(?<=[.!?])\s+/)
    .map((part) => part.trim())
    .filter(Boolean);

  const selected = (parts.length > 0 ? parts : [safeText]).slice(0, 2);
  const normalized = selected
    .join(" ")
    .replace(/\s+/g, " ")
    .trim();

  return normalized.slice(0, 800);
}

function requireProxyAuth(req, res, next) {
  const tokenFromAuthHeader = extractBearerToken(req.headers.authorization);
  const tokenFromApiKeyHeader = sanitizeText(String(req.headers["x-api-key"] || ""), 200);
  const providedToken = tokenFromAuthHeader || tokenFromApiKeyHeader;

  if (!AI_PROXY_TOKEN || !providedToken || providedToken !== AI_PROXY_TOKEN) {
    return res.status(401).json({ error: "Không được phép truy cập." });
  }

  return next();
}

function extractBearerToken(authorizationHeader) {
  if (!authorizationHeader || typeof authorizationHeader !== "string") {
    return "";
  }

  const [scheme, token] = authorizationHeader.trim().split(/\s+/, 2);
  if (!scheme || !token || scheme.toLowerCase() !== "bearer") {
    return "";
  }

  return sanitizeText(token, 200);
}

function createCorsMiddleware() {
  const allowedOrigins = resolveAllowedOrigins();

  return (req, res, next) => {
    const origin = req.headers.origin;

    if (origin) {
      if (!isCorsRouteAllowed(req.method, req.path)) {
        return res.status(403).json({ error: "Đường dẫn không hỗ trợ CORS." });
      }

      if (!allowedOrigins.has(origin)) {
        return res.status(403).json({ error: "Nguồn gốc không được cho phép." });
      }

      res.setHeader("Access-Control-Allow-Origin", origin);
      res.setHeader("Vary", "Origin");
      res.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS.join(", "));
      res.setHeader("Access-Control-Allow-Headers", ALLOWED_HEADERS.join(", "));
      res.setHeader("Access-Control-Max-Age", "600");
    }

    if (req.method === "OPTIONS") {
      return res.status(204).send();
    }

    return next();
  };
}

function resolveAllowedOrigins() {
  const configuredOrigins = String(process.env.ALLOWED_ORIGINS || "")
    .split(",")
    .map((origin) => origin.trim())
    .filter(Boolean);

  if (configuredOrigins.length > 0) {
    return new Set(configuredOrigins);
  }

  const isDevelopment = (process.env.NODE_ENV || "development") !== "production";
  if (!isDevelopment) {
    return new Set();
  }

  return new Set([
    "http://localhost:3000",
    "http://localhost:5173",
    "http://127.0.0.1:3000",
    "http://127.0.0.1:5173"
  ]);
}

function isCorsRouteAllowed(method, path) {
  if (path === "/health" && (method === "GET" || method === "OPTIONS")) {
    return true;
  }

  if (path === "/ai/recommendation" && (method === "POST" || method === "OPTIONS")) {
    return true;
  }

  if (path === "/ai/chat" && (method === "POST" || method === "OPTIONS")) {
    return true;
  }

  if (path === "/ai/scan-dish" && (method === "POST" || method === "OPTIONS")) {
    return true;
  }

  if (path === "/catalog/branches" && (method === "GET" || method === "OPTIONS")) {
    return true;
  }

  if (path === "/catalog/external-dishes" && (method === "GET" || method === "OPTIONS")) {
    return true;
  }

  if (path === "/proxy-image" && (method === "GET" || method === "OPTIONS")) {
    return true;
  }

  return false;
}
