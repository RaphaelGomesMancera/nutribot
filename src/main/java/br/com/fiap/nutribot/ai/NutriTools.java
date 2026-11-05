package br.com.fiap.nutribot.ai;

import java.util.List;

import br.com.fiap.nutribot.dto.MealEntryResponse;
import br.com.fiap.nutribot.dto.SummaryResponse;
import br.com.fiap.nutribot.model.Food;
import br.com.fiap.nutribot.model.MealType;
import br.com.fiap.nutribot.service.FoodService;
import br.com.fiap.nutribot.service.MealService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

// Ferramentas que o modelo pode chamar. Todas reaproveitam os services do projeto base.
@Component
@RequiredArgsConstructor
public class NutriTools {

    private final FoodService foodService;
    private final MealService mealService;

    @Tool(description = """
            Busca alimentos na tabela da clínica pelo nome e devolve calorias, proteínas, carboidratos
            e gorduras por 100 g. Use sempre que precisar de valores nutricionais de um alimento.""")
    public List<Food> findFood(
            @ToolParam(description = "Nome do alimento, no singular e sem marca. Ex.: tapioca, ovo, arroz") String name) {
        return foodService.search(name).stream().limit(10).toList();
    }

    @Tool(description = """
            Devolve o resumo do dia do paciente: calorias consumidas, macronutrientes, meta diária,
            calorias restantes e as refeições registradas. Use para perguntas sobre quanto já comeu
            ou quanto ainda pode comer hoje.""")
    public SummaryResponse getDaySummary() {
        return mealService.summary(null);
    }

    @Tool(description = """
            Registra no diário alimentar de hoje uma refeição que o paciente disse que comeu.
            Só chame quando o paciente afirmar que comeu algo. Devolve a refeição registrada.""")
    public MealEntryResponse logMeal(
            @ToolParam(description = "Nome do alimento como na tabela. Ex.: ovo, banana, arroz") String foodName,
            @ToolParam(description = "Quantidade em gramas (estime se o paciente falou em unidades)") double grams,
            @ToolParam(description = "Refeição: BREAKFAST, LUNCH, DINNER ou SNACK") MealType mealType) {
        return MealEntryResponse.from(mealService.log(foodName, grams, mealType));
    }
}
