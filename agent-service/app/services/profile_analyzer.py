from app.schemas.profile import ProfileAnalyzeRequest, ProfileAnalyzeResponse


def analyze_profile(request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
    return ProfileAnalyzeResponse(
        currentSkills=[
            "Python basics",
            "Backend development fundamentals",
            "REST API design",
        ],
        strengths=[
            "Clear learning goal",
            "Consistent daily time budget",
            "Software engineering foundation",
        ],
        weaknesses=[
            "AI agent workflow design",
            "LLM application evaluation",
            "End-to-end project integration",
        ],
        recommendedDirection=(
            "Build a FastAPI-based AI planning service first, then connect it with the "
            f"backend workflow for the goal: {request.goal}."
        ),
    )
