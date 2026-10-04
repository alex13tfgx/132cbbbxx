-- 67 (Six Seven) brainrot --

vanilla_model.PLAYER:setVisible(false)
vanilla_model.ARMOR:setVisible(false)
vanilla_model.CAPE:setVisible(false)
vanilla_model.ELYTRA:setVisible(false)

local body = models.model.Body
local digits = models.model.Digits

-- Жест «six seven»: руки вытянуты вперёд ладонями вверх и по очереди качаются вверх-вниз.
function events.render(delta)
	local t = world.getTime() + delta
	local walking = player:getVelocity().xz:length() > 0.05
	local speed = walking and 0.55 or 0.4
	local swing = math.sin(t * speed) * 22

	body.ArmR:setRot(75 + swing, 0, -6)
	body.ArmL:setRot(75 - swing, 0, 6)

	-- Цифры 6 и 7 крутятся и покачиваются над головой, каждая в свою сторону.
	digits:setPos(0, math.sin(t * 0.12) * 1.5, 0)
	digits:setRot(0, (t * 3) % 360, 0)
	digits.Digit6:setPos(0, math.max(0, math.sin(t * speed)) * 1.5, 0)
	digits.Digit7:setPos(0, math.max(0, -math.sin(t * speed)) * 1.5, 0)
end
