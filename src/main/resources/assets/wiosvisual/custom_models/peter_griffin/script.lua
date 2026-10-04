vanilla_model.ARMOR:setVisible(false)
vanilla_model.PLAYER:setVisible(false)

function events.render()
    animations.Peter_Griffin.keypose:play()
    if player:getPose() == "CROUCHING" then
        animations.Peter_Griffin.crouch:play()
    end
end

local function deathPoseFunc(state)
    animations.Peter_Griffin.death:setPlaying(state)

end

local function birdWordFunc(state)
    animations.Peter_Griffin.bird:setPlaying(state)

end


local mainPage = action_wheel:newPage()
action_wheel:setPage(mainPage)

local deathPose = mainPage:newAction():title("Death Pose"):setOnToggle(deathPoseFunc)
local birdWord = mainPage:newAction():title("Bird is the Word"):setOnToggle(birdWordFunc)

