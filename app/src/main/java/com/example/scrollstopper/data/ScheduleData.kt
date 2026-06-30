package com.example.scrollstopper.data

object ScheduleData {
    val ranks = listOf(
        Rank("Rookie", "Starting the journey", 0),
        Rank("Aspirant", "Gaining momentum", 150),
        Rank("Grinder", "Embracing the daily routine", 400),
        Rank("Strategist", "Solving complex concepts", 800),
        Rank("Topper", "Excellence in practice", 1400),
        Rank("GATE Qualifier", "Ready for GATE 2027", 2200)
    )

    val weeks = listOf(
        WeekPlan(1, "Phase 1: Foundations", "Engineering Mathematics", "Linear Algebra", 
            "Ravindrababu Ravula (RBR) - Linear Algebra", "gateoverflow.in - Linear Algebra PYQs"),
        WeekPlan(2, "Phase 1: Foundations", "Engineering Mathematics", "Calculus & Limits", 
            "Ravindrababu Ravula (RBR) - Calculus", "gateoverflow.in - Calculus PYQs"),
        WeekPlan(3, "Phase 1: Foundations", "Engineering Mathematics", "Differential Equations", 
            "Ravindrababu Ravula (RBR) - Differential Equations", "gateoverflow.in - DEs PYQs"),
        WeekPlan(4, "Phase 1: Foundations", "Engineering Mathematics", "Probability & Stats", 
            "Ravindrababu Ravula (RBR) - Probability", "gateoverflow.in - Probability PYQs"),
        
        WeekPlan(5, "Phase 1: Foundations", "Network Theory", "Network Basics & KVL/KCL", 
            "Gate Smashers - Network Theory Introduction", "gateoverflow.in - KVL KCL PYQs"),
        WeekPlan(6, "Phase 1: Foundations", "Network Theory", "Theorems & Mesh/Nodal", 
            "Gate Smashers - Network Theorems", "gateoverflow.in - Network Theorems PYQs"),
        WeekPlan(7, "Phase 1: Foundations", "Network Theory", "Transient Analysis & AC Circuits", 
            "Gate Smashers - Transient Analysis", "gateoverflow.in - Transients PYQs"),
        WeekPlan(8, "Phase 1: Foundations", "Network Theory", "Two-Port Networks & Resonance", 
            "Gate Smashers - Two Port Networks", "gateoverflow.in - Two Port PYQs"),
        
        WeekPlan(9, "Phase 1: Foundations", "Signals & Systems", "Continuous & Discrete Time Signals", 
            "Neso Academy - Signals and Systems", "gateoverflow.in - Signals Classification PYQs"),
        WeekPlan(10, "Phase 1: Foundations", "Signals & Systems", "Fourier & Laplace Transforms", 
            "Neso Academy - LTI Systems & Transforms", "gateoverflow.in - Transforms PYQs"),

        WeekPlan(11, "Phase 2: Core Heavy", "Electrical Machines", "DC Machines Principles", 
            "Gate Smashers - DC Generators & Motors", "gateoverflow.in - DC Machines PYQs"),
        WeekPlan(12, "Phase 2: Core Heavy", "Electrical Machines", "Transformers (Single & Three Phase)", 
            "Gate Smashers - Transformers", "gateoverflow.in - Transformers PYQs"),
        WeekPlan(13, "Phase 2: Core Heavy", "Electrical Machines", "Induction & Synchronous Machines", 
            "Gate Smashers - AC Machines", "gateoverflow.in - AC Machines PYQs"),

        WeekPlan(14, "Phase 2: Core Heavy", "Power Systems", "Generation & Transmission Line Parameters", 
            "Gate Smashers - Power System Transmission", "gateoverflow.in - Transmission Line PYQs"),
        WeekPlan(15, "Phase 2: Core Heavy", "Power Systems", "Load Flow Studies & Fault Analysis", 
            "Gate Smashers - Fault Analysis", "gateoverflow.in - Load Flow PYQs"),
        WeekPlan(16, "Phase 2: Core Heavy", "Power Systems", "System Protection & Stability", 
            "Gate Smashers - Power System Protection", "gateoverflow.in - PS Stability PYQs"),

        WeekPlan(17, "Phase 2: Core Heavy", "Control Systems", "Transfer Function & Block Diagram", 
            "Gate Smashers - Control Systems", "gateoverflow.in - Signal Flow Graph PYQs"),
        WeekPlan(18, "Phase 2: Core Heavy", "Control Systems", "Time & Frequency Response Stability", 
            "Gate Smashers - Bode Plot & Nyquist", "gateoverflow.in - Routh Hurwitz Stability PYQs"),

        WeekPlan(19, "Phase 2: Core Heavy", "Power Electronics", "Diode & SCR Rectifiers", 
            "Gate Smashers - Rectifiers & Thyristors", "gateoverflow.in - Power Diodes PYQs"),
        WeekPlan(20, "Phase 2: Core Heavy", "Power Electronics", "Choppers, Inverters & SMPS", 
            "Gate Smashers - DC-DC & AC-DC Converters", "gateoverflow.in - Inverters PYQs"),

        WeekPlan(21, "Phase 3: Strengthening", "Measurements", "Error Analysis & Indicating Instruments", 
            "Gate Smashers - PMMC & MI Meters", "gateoverflow.in - Instruments PYQs"),
        WeekPlan(22, "Phase 3: Strengthening", "Measurements", "Bridges, Potentiometers & Oscilloscopes", 
            "Gate Smashers - AC Bridges & CRO", "gateoverflow.in - Measurement Bridges PYQs"),

        WeekPlan(23, "Phase 3: Strengthening", "Analog Electronics", "Diodes, BJTs & Op-Amps", 
            "Neso Academy - BJT & Operational Amplifiers", "gateoverflow.in - Op-Amp Circuits PYQs"),
        WeekPlan(24, "Phase 3: Strengthening", "Digital Electronics", "Combinational, Sequential & ADCs", 
            "Neso Academy - Logic Gates & Counters", "gateoverflow.in - Sequential Circuits PYQs"),

        WeekPlan(25, "Phase 3: Strengthening", "General Aptitude", "Quantitative & Verbal Reasoning", 
            "Ravindrababu Ravula (RBR) - Aptitude", "gateoverflow.in - General Aptitude PYQs"),
        WeekPlan(26, "Phase 3: Strengthening", "Weak Topic Buffer", "Weak topics backlog clearing", 
            "Neso / Gate Smashers / RBR - Alternate reviews", "Your Weak Topics Log & Error Sheets"),

        WeekPlan(27, "Phase 4: Revision & Mocks", "Full Syllabus Revision", "Maths, Networks, Signals revision", 
            "Fast-paced crash courses / Own notes", "gateoverflow.in - Mixed Subject Tests"),
        WeekPlan(28, "Phase 4: Revision & Mocks", "Full Syllabus Revision", "Machines, Power Systems, Controls revision", 
            "Quick revision modules / Formula review", "gateoverflow.in - Core Heavy Mixed Tests"),
        WeekPlan(29, "Phase 4: Revision & Mocks", "Full Syllabus Revision", "Measurements, Analog, Digital revision", 
            "Flashcards / Formula sheet compilation", "gateoverflow.in - Electronics & Measurements PYQs"),

        WeekPlan(30, "Phase 4: Revision & Mocks", "Full Mocks Sprint", "Full syllabus mock tests", 
            "Reviewing previous mistakes & Mock Strategy", "Anubhav by Made Easy - Test 1 & 2"),
        WeekPlan(31, "Phase 4: Revision & Mocks", "Full Mocks Sprint", "Official paper simulation", 
            "GATE Official PYQ Papers analysis", "Official IIT GOAPS Mock Platform"),
        WeekPlan(32, "Phase 4: Revision & Mocks", "Full Mocks Sprint", "Final mock diagnostic", 
            "Weakness analysis & speed optimization", "Anubhav Mock 3 / SelfStudys Full Mocks"),
        WeekPlan(33, "Phase 4: Revision & Mocks", "Final Week", "Formula sheets only, rest & sleep", 
            "Relaxation / Mind prep / Formula glance", "No active tests - Rest / Sleep consistency")
    )
}

data class Rank(
    val name: String,
    val description: String,
    val xpRequired: Int
)
